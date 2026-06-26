package org.catmunity.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.catmunity.model.entity.PostLike;
import org.catmunity.model.vo.PostVO;

public interface PostLikeService extends IService<PostLike> {

    PostVO likePost(Long postId);

    PostVO unlikePost(Long postId);

    Boolean isLiked(Long postId);
}