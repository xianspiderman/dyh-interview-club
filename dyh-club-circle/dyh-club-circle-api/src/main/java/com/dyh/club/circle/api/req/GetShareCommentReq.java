package com.dyh.club.circle.api.req;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * <p>
 * 鸡圈内容信息
 * </p>
 *
 * @since 2024/05/16
 */
@Getter
@Setter
public class GetShareCommentReq implements Serializable {

    private Long id;

}
