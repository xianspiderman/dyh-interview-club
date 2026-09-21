package com.dyh.club.platform.community;

import com.dyh.club.platform.common.BizException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.*;

@Service
public class CommunityService {
    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate namedJdbc;
    private final SensitiveWordService sensitive;

    public CommunityService(JdbcTemplate jdbc, SensitiveWordService sensitive) {
        this.jdbc=jdbc; this.namedJdbc=new NamedParameterJdbcTemplate(jdbc); this.sensitive=sensitive;
    }

    public List<Map<String,Object>> circles() {
        List<Map<String,Object>> flat=jdbc.query("SELECT id,parent_id,name,sort_no FROM club_circle WHERE status='ENABLED' ORDER BY sort_no,id", this::circleRow);
        Map<Long,List<Map<String,Object>>> children=new HashMap<>(); List<Map<String,Object>> roots=new ArrayList<>();
        for(Map<String,Object> item:flat){Object p=item.get("parentId"); if(p==null) roots.add(item); else children.computeIfAbsent(((Number)p).longValue(),k->new ArrayList<>()).add(item);}
        for(Map<String,Object> root:roots) root.put("children",children.getOrDefault(((Number)root.get("id")).longValue(),Collections.emptyList()));
        return roots;
    }

    public Map<String,Object> posts(Long circleId,int page,int size) {
        int actual=Math.max(1,Math.min(size,20)); int offset=(Math.max(1,page)-1)*actual;
        List<Object> args=new ArrayList<>(); String where=" WHERE p.status='VISIBLE'";
        if(circleId!=null){where+=" AND p.circle_id=?";args.add(circleId);}
        Integer total=jdbc.queryForObject("SELECT COUNT(*) FROM club_post p"+where,Integer.class,args.toArray());
        args.add(actual);args.add(offset);
        List<Map<String,Object>> rows=jdbc.query("SELECT p.id,p.circle_id,p.title,p.content,p.comment_count,p.like_count,p.created_at,u.nickname,u.avatar FROM club_post p JOIN club_user u ON u.id=p.author_id"+where+" ORDER BY p.id DESC LIMIT ? OFFSET ?",this::postRow,args.toArray());
        Map<String,Object> result=new LinkedHashMap<>();result.put("records",rows);result.put("total",total);result.put("page",page);result.put("size",actual);return result;
    }

    public Map<String,Object> post(long id) {
        try{return jdbc.queryForObject("SELECT p.id,p.circle_id,p.title,p.content,p.comment_count,p.like_count,p.created_at,u.nickname,u.avatar FROM club_post p JOIN club_user u ON u.id=p.author_id WHERE p.id=? AND p.status='VISIBLE'",this::postRow,id);}
        catch(org.springframework.dao.EmptyResultDataAccessException e){throw BizException.notFound("帖子不存在");}
    }

    @Transactional
    public long createPost(long userId, PostRequest request) {
        requireText(request.title,"标题",200);requireText(request.content,"正文",10000);sensitive.assertAllowed(request.title+" "+request.content);
        Integer circle=jdbc.queryForObject("SELECT COUNT(*) FROM club_circle WHERE id=? AND status='ENABLED'",Integer.class,request.circleId);
        if(circle==null||circle==0)throw BizException.badRequest("圈子不存在");
        KeyHolder key=new GeneratedKeyHolder();
        jdbc.update(c->{PreparedStatement s=c.prepareStatement("INSERT INTO club_post(circle_id,author_id,title,content,status) VALUES(?,?,?,?,'VISIBLE')",new String[]{"id"});s.setLong(1,request.circleId);s.setLong(2,userId);s.setString(3,request.title.trim());s.setString(4,request.content.trim());return s;},key);
        return Objects.requireNonNull(key.getKey()).longValue();
    }

    @Transactional
    public long createComment(long userId, CommentRequest request) {
        requireText(request.content,"评论内容",2000);sensitive.assertAllowed(request.content);
        String type=normalizeBiz(request.bizType); long rootId; Long parentId=request.parentId; Long toUser=null;
        if("POST".equals(type)){Integer n=jdbc.queryForObject("SELECT COUNT(*) FROM club_post WHERE id=? AND status='VISIBLE'",Integer.class,request.bizId);if(n==null||n==0)throw BizException.notFound("帖子不存在");}
        if(parentId!=null){Map<String,Object> parent;
            try{parent=jdbc.queryForMap("SELECT id,biz_type,biz_id,root_id,author_id FROM club_comment WHERE id=? AND status='VISIBLE'",parentId);}catch(org.springframework.dao.EmptyResultDataAccessException e){throw BizException.badRequest("被回复的评论不存在");}
            if(!type.equals(parent.get("biz_type"))||request.bizId!=((Number)parent.get("biz_id")).longValue())throw BizException.badRequest("回复关系不属于当前内容");
            Object root=parent.get("root_id");rootId=root==null?parentId:((Number)root).longValue();toUser=((Number)parent.get("author_id")).longValue();
        }else rootId=0;
        final long finalRoot=rootId;final Long finalToUser=toUser;
        KeyHolder key=new GeneratedKeyHolder();
        jdbc.update(c->{PreparedStatement s=c.prepareStatement("INSERT INTO club_comment(biz_type,biz_id,parent_id,root_id,author_id,to_user_id,content,status) VALUES(?,?,?,?,?,?,?,'VISIBLE')",new String[]{"id"});s.setString(1,type);s.setLong(2,request.bizId);if(parentId==null)s.setObject(3,null);else s.setLong(3,parentId);if(parentId==null)s.setObject(4,null);else s.setLong(4,finalRoot);s.setLong(5,userId);if(finalToUser==null)s.setObject(6,null);else s.setLong(6,finalToUser);s.setString(7,request.content.trim());return s;},key);
        long id=Objects.requireNonNull(key.getKey()).longValue(); if(parentId==null)jdbc.update("UPDATE club_comment SET root_id=? WHERE id=?",id,id);
        if("POST".equals(type))jdbc.update("UPDATE club_post SET comment_count=comment_count+1,updated_at=CURRENT_TIMESTAMP WHERE id=?",request.bizId);
        return id;
    }

    public Map<String,Object> comments(CommentQuery query) {
        int size=Math.max(1,Math.min(query.size<=0?20:query.size,20));String type=normalizeBiz(query.bizType);
        List<Object> args=new ArrayList<>();String cursor="";args.add(type);args.add(query.bizId);
        if(query.cursorTime!=null&&query.cursorId!=null){cursor=" AND (c.created_at<? OR (c.created_at=? AND c.id<?))";Timestamp t=Timestamp.valueOf(query.cursorTime);args.add(t);args.add(t);args.add(query.cursorId);}
        args.add(size);
        List<Map<String,Object>> roots=jdbc.query("SELECT c.id,c.biz_type,c.biz_id,c.parent_id,c.root_id,c.content,c.created_at,u.id user_id,u.nickname,u.avatar FROM club_comment c JOIN club_user u ON u.id=c.author_id WHERE c.biz_type=? AND c.biz_id=? AND c.parent_id IS NULL AND c.status='VISIBLE'"+cursor+" ORDER BY c.created_at DESC,c.id DESC LIMIT ?",this::commentRow,args.toArray());
        if(roots.isEmpty())return pageComments(roots,null);
        List<Long> rootIds=new ArrayList<>();for(Map<String,Object> root:roots)rootIds.add(((Number)root.get("id")).longValue());
        MapSqlParameterSource params=new MapSqlParameterSource("roots",rootIds);
        List<Map<String,Object>> replies=namedJdbc.query("SELECT * FROM (SELECT c.id,c.biz_type,c.biz_id,c.parent_id,c.root_id,c.content,c.created_at,u.id user_id,u.nickname,u.avatar,tu.nickname to_nickname,ROW_NUMBER() OVER(PARTITION BY c.root_id ORDER BY c.created_at DESC,c.id DESC) rn,COUNT(*) OVER(PARTITION BY c.root_id) reply_total FROM club_comment c JOIN club_user u ON u.id=c.author_id LEFT JOIN club_user tu ON tu.id=c.to_user_id WHERE c.root_id IN (:roots) AND c.parent_id IS NOT NULL AND c.status='VISIBLE') x WHERE rn<=3 ORDER BY root_id,created_at,id",params,this::replyRow);
        Map<Long,List<Map<String,Object>>> grouped=new HashMap<>();Map<Long,Long> totals=new HashMap<>();for(Map<String,Object> reply:replies){long root=((Number)reply.get("rootId")).longValue();grouped.computeIfAbsent(root,k->new ArrayList<>()).add(reply);totals.put(root,((Number)reply.get("replyTotal")).longValue());}
        for(Map<String,Object> root:roots){long id=((Number)root.get("id")).longValue();root.put("replies",grouped.getOrDefault(id,Collections.emptyList()));root.put("replyTotal",totals.getOrDefault(id,0L));}
        Map<String,Object> last=roots.get(roots.size()-1);return pageComments(roots,String.valueOf(last.get("createdAt"))+"|"+last.get("id"));
    }

    public Map<String,Object> replies(long rootId,int page,int size) {
        int actual=Math.max(1,Math.min(size<=0?20:size,20));int offset=(Math.max(1,page)-1)*actual;
        List<Map<String,Object>> rows=jdbc.query("SELECT c.id,c.biz_type,c.biz_id,c.parent_id,c.root_id,c.content,c.created_at,u.id user_id,u.nickname,u.avatar,tu.nickname to_nickname FROM club_comment c JOIN club_user u ON u.id=c.author_id LEFT JOIN club_user tu ON tu.id=c.to_user_id WHERE c.root_id=? AND c.parent_id IS NOT NULL AND c.status='VISIBLE' ORDER BY c.created_at,c.id LIMIT ? OFFSET ?",this::replyRow,rootId,actual,offset);
        Integer total=jdbc.queryForObject("SELECT COUNT(*) FROM club_comment WHERE root_id=? AND parent_id IS NOT NULL AND status='VISIBLE'",Integer.class,rootId);
        Map<String,Object> out=new LinkedHashMap<>();out.put("records",rows);out.put("total",total);out.put("page",page);out.put("size",actual);return out;
    }

    @Transactional
    public void deleteComment(long userId,long id,boolean admin) {
        Map<String,Object> comment;try{comment=jdbc.queryForMap("SELECT biz_type,biz_id,author_id,status FROM club_comment WHERE id=?",id);}catch(org.springframework.dao.EmptyResultDataAccessException e){throw BizException.notFound("评论不存在");}
        if(!admin&&userId!=((Number)comment.get("author_id")).longValue())throw BizException.forbidden("只能删除自己的评论");
        if(jdbc.update("UPDATE club_comment SET status='DELETED' WHERE id=? AND status='VISIBLE'",id)==1&&"POST".equals(comment.get("biz_type")))jdbc.update("UPDATE club_post SET comment_count=CASE WHEN comment_count>0 THEN comment_count-1 ELSE 0 END WHERE id=?",comment.get("biz_id"));
    }

    private Map<String,Object> circleRow(java.sql.ResultSet rs,int n)throws java.sql.SQLException{Map<String,Object>m=new LinkedHashMap<>();m.put("id",rs.getLong("id"));long p=rs.getLong("parent_id");m.put("parentId",rs.wasNull()?null:p);m.put("name",rs.getString("name"));m.put("sortNo",rs.getInt("sort_no"));return m;}
    private Map<String,Object> postRow(java.sql.ResultSet rs,int n)throws java.sql.SQLException{Map<String,Object>m=new LinkedHashMap<>();m.put("id",rs.getLong("id"));m.put("circleId",rs.getLong("circle_id"));m.put("title",rs.getString("title"));m.put("content",rs.getString("content"));m.put("commentCount",rs.getInt("comment_count"));m.put("likeCount",rs.getInt("like_count"));m.put("createdAt",rs.getTimestamp("created_at"));m.put("authorName",rs.getString("nickname"));m.put("authorAvatar",rs.getString("avatar"));return m;}
    private Map<String,Object> commentRow(java.sql.ResultSet rs,int n)throws java.sql.SQLException{Map<String,Object>m=new LinkedHashMap<>();m.put("id",rs.getLong("id"));m.put("bizType",rs.getString("biz_type"));m.put("bizId",rs.getLong("biz_id"));long p=rs.getLong("parent_id");m.put("parentId",rs.wasNull()?null:p);long root=rs.getLong("root_id");m.put("rootId",rs.wasNull()?null:root);m.put("content",rs.getString("content"));m.put("createdAt",rs.getTimestamp("created_at"));m.put("userId",rs.getLong("user_id"));m.put("nickname",rs.getString("nickname"));m.put("avatar",rs.getString("avatar"));return m;}
    private Map<String,Object> replyRow(java.sql.ResultSet rs,int n)throws java.sql.SQLException{Map<String,Object>m=commentRow(rs,n);m.put("toNickname",rs.getString("to_nickname"));try{m.put("replyTotal",rs.getLong("reply_total"));}catch(java.sql.SQLException ignored){}return m;}
    private Map<String,Object> pageComments(List<Map<String,Object>>records,String next){Map<String,Object>m=new LinkedHashMap<>();m.put("records",records);m.put("nextCursor",next);return m;}
    private String normalizeBiz(String type){String v=type==null?"":type.toUpperCase(Locale.ROOT);if(!Arrays.asList("POST","QUESTION").contains(v))throw BizException.badRequest("评论对象类型不合法");return v;}
    private void requireText(String value,String label,int max){if(value==null||value.trim().isEmpty())throw BizException.badRequest(label+"不能为空");if(value.trim().length()>max)throw BizException.badRequest(label+"不能超过 "+max+" 个字符");}

    public static class PostRequest{public long circleId;public String title;public String content;}
    public static class CommentRequest{public String bizType;public long bizId;public Long parentId;public String content;}
    public static class CommentQuery{public String bizType;public long bizId;public String cursorTime;public Long cursorId;public int size=20;}
}
