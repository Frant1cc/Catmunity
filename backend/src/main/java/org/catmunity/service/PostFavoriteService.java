package org.catmunity.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.catmunity.model.entity.PostFavorite;
import org.catmunity.model.vo.PostVO;
import org.catmunity.result.PageResult;

public interface PostFavoriteService extends IService<PostFavorite> {

    PostVO favoritePost(Long postId);

    PostVO unfavoritePost(Long postId);

    Boolean isFavorited(Long postId);

    PageResult<PostVO> getMyFavorites(Integer pageNum, Integer pageSize);
}