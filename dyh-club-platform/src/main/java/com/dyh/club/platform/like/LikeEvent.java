package com.dyh.club.platform.like;

public class LikeEvent {
    public long questionId;
    public long userId;
    public boolean liked;
    public long version;

    public LikeEvent() {}
    public LikeEvent(long questionId,long userId,boolean liked,long version){this.questionId=questionId;this.userId=userId;this.liked=liked;this.version=version;}
    public String businessKey(){return questionId+":"+userId;}
}
