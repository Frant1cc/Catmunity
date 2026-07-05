package org.catmunity.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.catmunity.model.dto.PostDTO;
import org.catmunity.model.entity.Post;
import org.catmunity.model.vo.PostVO;
import org.catmunity.result.PageResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PostService extends IService<Post> {

    PostVO createPost(PostDTO postDTO);

    PostVO getPostById(Long id);

    PageResult<PostVO> getPostPage(Integer pageNum, Integer pageSize, String sortBy);

    List<PostVO> getUserPosts(Long userId);

    List<PostVO> getFollowingPosts(Integer pageNum, Integer pageSize);

    List<PostVO> getPostsByTag(String tag, Integer pageNum, Integer pageSize);

    PostVO updatePost(PostDTO postDTO);

    void deletePost(Long id);

    List<String> uploadPostImages(MultipartFile[] images);
}