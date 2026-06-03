-- flyway:executeInTransaction=false
-- New indexes for *_utc read paths. CONCURRENTLY avoids blocking writes during production rollout.

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_posts_created_at_utc ON posts(created_at_utc DESC);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_posts_cursor_utc ON posts(created_at_utc DESC, id DESC);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_posts_updated_at_utc ON posts(updated_at_utc DESC, id DESC);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_posts_stats_sync_utc ON posts(stats_updated_at_utc, updated_at_utc);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_comments_created_at_utc ON comments(created_at_utc);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_comment_like_log_comment_created_utc ON comment_like_log(comment_id, created_at_utc DESC);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_comment_like_log_created_utc ON comment_like_log(created_at_utc DESC);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_post_view_log_post_created_utc ON post_view_log(post_id, created_at_utc DESC);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_post_view_log_created_utc ON post_view_log(created_at_utc DESC);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_post_like_log_post_created_utc ON post_like_log(post_id, created_at_utc DESC);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_post_like_log_created_utc ON post_like_log(created_at_utc DESC);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_notification_recipients_user_id_created_at_utc ON notification_recipients(user_id, created_at_utc DESC);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_notification_recipients_user_id_read_at_utc ON notification_recipients(user_id, read_at_utc);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_notifications_type_created_at_utc ON notifications(type, created_at_utc DESC);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_links_published_at_utc ON links(published_at_utc DESC);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_link_view_log_link_created_utc ON link_view_log(link_id, created_at_utc DESC);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_link_view_log_created_utc ON link_view_log(created_at_utc DESC);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_link_like_log_link_created_utc ON link_like_log(link_id, created_at_utc DESC);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_link_like_log_created_utc ON link_like_log(created_at_utc DESC);
