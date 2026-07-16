package com.devteria.file.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FileMgmt {
    @MongoId
    String id;
    String ownerId;
    String fileName;
    String contentType;
    long size;
    String md5Checksum;
    String path;


}
