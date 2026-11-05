package com.line1.booktalk.global.storage;

/**
 * 사진 저장소. 해커톤에서는 서버 폴더(LocalFileStorage)를 쓰고,
 * 나중에 Supabase Storage나 S3로 바꿀 때는 이 인터페이스의 구현만 바꾸면 된다.
 */
public interface FileStorage {

    /** 파일을 저장하고, 나중에 찾을 때 쓰는 key를 돌려준다 */
    String save(String directory, String extension, byte[] content);

    byte[] load(String key);

    /** 파일이 없어도 오류 없이 넘어간다 */
    void delete(String key);
}
