package com.devteria.file.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

@Service
public class FileService {
    public Object uploadFile(MultipartFile file) throws IOException {
        Path forder = Paths.get("D:/");
        String fileExtension = StringUtils
                .getFilenameExtension(file.getOriginalFilename());
        String fileName = Objects.isNull(fileExtension) ?
                UUID.randomUUID().toString()
                : UUID.randomUUID().toString()+ "." + fileExtension;

        // gắn kết giưã gốc forder và tên đươnhf dẫn để tạo 1 đường dẫn lưu data
        Path filePath = forder.resolve(fileName).normalize().toAbsolutePath();
        //bơm dữ liệu từ client vào đường dãn mình đã taoj(filePath)
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return null;
    }
}
