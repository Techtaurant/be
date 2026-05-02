## Not Todo

- Swagger 성공 응답 스키마를 맞추기 위해 `*ApiResponse` 같은 Swagger 전용 wrapper DTO를 새로 만들지 않는다.
- 성공 응답은 Controller/Docs 메서드의 실제 반환 타입(`ApiResponse<SomeResponse>`)에서 SpringDoc이 자동 추론하도록 둔다.
- Swagger 테스트의 media type 경로를 맞추기 위해 Controller mapping에 불필요한 `produces`를 추가하지 않는다.
