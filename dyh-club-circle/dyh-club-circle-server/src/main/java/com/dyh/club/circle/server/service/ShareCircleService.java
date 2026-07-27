package com.dyh.club.circle.server.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.dyh.club.circle.api.req.RemoveShareCircleReq;
import com.dyh.club.circle.api.req.SaveShareCircleReq;
import com.dyh.club.circle.api.req.UpdateShareCircleReq;
import com.dyh.club.circle.api.vo.ShareCircleVO;
import com.dyh.club.circle.server.entity.po.ShareCircle;

import java.util.List;

/**
 * <p>
 * 圈子信息 服务类
 * </p>
 *
 * @since 2024/05/16
 */
public interface ShareCircleService extends IService<ShareCircle> {

    List<ShareCircleVO> listResult();

    Boolean saveCircle(SaveShareCircleReq req);

    Boolean updateCircle(UpdateShareCircleReq req);

    Boolean removeCircle(RemoveShareCircleReq req);
}
