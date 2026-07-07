package com.devteria.post.mapper;


import com.devteria.post.dto.response.PostResponse;
import com.devteria.post.entity.Post;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PostMapper {
    PostResponse toPostResponse(Post post);

}
