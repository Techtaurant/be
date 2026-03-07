-- 테스트 데이터 정리 스크립트
-- 각 테스트 메서드 실행 전에 실행되어 데이터베이스를 깨끗한 상태로 초기화합니다

-- 외래 키 제약 조건 순서에 따라 삭제
DELETE FROM comments;
DELETE FROM post_tags;
DELETE FROM post_pictures;
DELETE FROM posts;
DELETE FROM categories;
DELETE FROM tags;
DELETE FROM users;
