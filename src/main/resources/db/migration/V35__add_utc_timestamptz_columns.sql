-- flyway:executeInTransaction=false
-- Expand migration for UTC-safe absolute instants.
-- Old TIMESTAMP columns are kept for rolling deploy compatibility; new application code uses *_utc TIMESTAMPTZ columns.

-- Add nullable columns without defaults to avoid table rewrites.
ALTER TABLE attachments ADD COLUMN IF NOT EXISTS created_at_utc TIMESTAMPTZ;
ALTER TABLE attachments ADD COLUMN IF NOT EXISTS updated_at_utc TIMESTAMPTZ;
ALTER TABLE categories ADD COLUMN IF NOT EXISTS created_at_utc TIMESTAMPTZ;
ALTER TABLE categories ADD COLUMN IF NOT EXISTS updated_at_utc TIMESTAMPTZ;
ALTER TABLE comment_like_log ADD COLUMN IF NOT EXISTS created_at_utc TIMESTAMPTZ;
ALTER TABLE comment_like_log ADD COLUMN IF NOT EXISTS updated_at_utc TIMESTAMPTZ;
ALTER TABLE comments ADD COLUMN IF NOT EXISTS created_at_utc TIMESTAMPTZ;
ALTER TABLE comments ADD COLUMN IF NOT EXISTS updated_at_utc TIMESTAMPTZ;
ALTER TABLE link_crawl_batches ADD COLUMN IF NOT EXISTS created_at_utc TIMESTAMPTZ;
ALTER TABLE link_crawl_batches ADD COLUMN IF NOT EXISTS updated_at_utc TIMESTAMPTZ;
ALTER TABLE link_daily_stats ADD COLUMN IF NOT EXISTS created_at_utc TIMESTAMPTZ;
ALTER TABLE link_daily_stats ADD COLUMN IF NOT EXISTS updated_at_utc TIMESTAMPTZ;
ALTER TABLE link_like_log ADD COLUMN IF NOT EXISTS created_at_utc TIMESTAMPTZ;
ALTER TABLE link_like_log ADD COLUMN IF NOT EXISTS updated_at_utc TIMESTAMPTZ;
ALTER TABLE link_read_log ADD COLUMN IF NOT EXISTS created_at_utc TIMESTAMPTZ;
ALTER TABLE link_read_log ADD COLUMN IF NOT EXISTS updated_at_utc TIMESTAMPTZ;
ALTER TABLE link_view_log ADD COLUMN IF NOT EXISTS created_at_utc TIMESTAMPTZ;
ALTER TABLE link_view_log ADD COLUMN IF NOT EXISTS updated_at_utc TIMESTAMPTZ;
ALTER TABLE links ADD COLUMN IF NOT EXISTS created_at_utc TIMESTAMPTZ;
ALTER TABLE links ADD COLUMN IF NOT EXISTS updated_at_utc TIMESTAMPTZ;
ALTER TABLE notification_arguments ADD COLUMN IF NOT EXISTS created_at_utc TIMESTAMPTZ;
ALTER TABLE notification_arguments ADD COLUMN IF NOT EXISTS updated_at_utc TIMESTAMPTZ;
ALTER TABLE notification_recipients ADD COLUMN IF NOT EXISTS created_at_utc TIMESTAMPTZ;
ALTER TABLE notification_recipients ADD COLUMN IF NOT EXISTS updated_at_utc TIMESTAMPTZ;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS created_at_utc TIMESTAMPTZ;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS updated_at_utc TIMESTAMPTZ;
ALTER TABLE post_daily_stats ADD COLUMN IF NOT EXISTS created_at_utc TIMESTAMPTZ;
ALTER TABLE post_daily_stats ADD COLUMN IF NOT EXISTS updated_at_utc TIMESTAMPTZ;
ALTER TABLE post_like_log ADD COLUMN IF NOT EXISTS created_at_utc TIMESTAMPTZ;
ALTER TABLE post_like_log ADD COLUMN IF NOT EXISTS updated_at_utc TIMESTAMPTZ;
ALTER TABLE post_read_log ADD COLUMN IF NOT EXISTS created_at_utc TIMESTAMPTZ;
ALTER TABLE post_read_log ADD COLUMN IF NOT EXISTS updated_at_utc TIMESTAMPTZ;
ALTER TABLE post_view_log ADD COLUMN IF NOT EXISTS created_at_utc TIMESTAMPTZ;
ALTER TABLE post_view_log ADD COLUMN IF NOT EXISTS updated_at_utc TIMESTAMPTZ;
ALTER TABLE posts ADD COLUMN IF NOT EXISTS created_at_utc TIMESTAMPTZ;
ALTER TABLE posts ADD COLUMN IF NOT EXISTS updated_at_utc TIMESTAMPTZ;
ALTER TABLE tags ADD COLUMN IF NOT EXISTS created_at_utc TIMESTAMPTZ;
ALTER TABLE tags ADD COLUMN IF NOT EXISTS updated_at_utc TIMESTAMPTZ;
ALTER TABLE user_bans ADD COLUMN IF NOT EXISTS created_at_utc TIMESTAMPTZ;
ALTER TABLE user_bans ADD COLUMN IF NOT EXISTS updated_at_utc TIMESTAMPTZ;
ALTER TABLE user_follows ADD COLUMN IF NOT EXISTS created_at_utc TIMESTAMPTZ;
ALTER TABLE user_follows ADD COLUMN IF NOT EXISTS updated_at_utc TIMESTAMPTZ;
ALTER TABLE user_links ADD COLUMN IF NOT EXISTS created_at_utc TIMESTAMPTZ;
ALTER TABLE user_links ADD COLUMN IF NOT EXISTS updated_at_utc TIMESTAMPTZ;
ALTER TABLE user_tokens ADD COLUMN IF NOT EXISTS created_at_utc TIMESTAMPTZ;
ALTER TABLE user_tokens ADD COLUMN IF NOT EXISTS updated_at_utc TIMESTAMPTZ;
ALTER TABLE users ADD COLUMN IF NOT EXISTS created_at_utc TIMESTAMPTZ;
ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_at_utc TIMESTAMPTZ;
ALTER TABLE comments ADD COLUMN IF NOT EXISTS deleted_at_utc TIMESTAMPTZ;
ALTER TABLE link_crawl_batches ADD COLUMN IF NOT EXISTS last_triggered_at_utc TIMESTAMPTZ;
ALTER TABLE links ADD COLUMN IF NOT EXISTS published_at_utc TIMESTAMPTZ;
ALTER TABLE notification_recipients ADD COLUMN IF NOT EXISTS read_at_utc TIMESTAMPTZ;
ALTER TABLE posts ADD COLUMN IF NOT EXISTS stats_updated_at_utc TIMESTAMPTZ;

-- Generic bidirectional sync trigger for old TIMESTAMP columns and new TIMESTAMPTZ *_utc columns.
CREATE OR REPLACE FUNCTION sync_utc_temporal_columns()
RETURNS trigger AS $$
DECLARE
    arg_index integer := 0;
    old_column_name text;
    utc_column_name text;
    old_column_value timestamp;
    utc_column_value timestamptz;
    patch jsonb := '{}'::jsonb;
    new_row jsonb;
    old_row jsonb;
BEGIN
    new_row := to_jsonb(NEW);
    IF TG_OP = 'UPDATE' THEN
        old_row := to_jsonb(OLD);
    END IF;

    WHILE arg_index < TG_NARGS LOOP
        old_column_name := TG_ARGV[arg_index];
        utc_column_name := TG_ARGV[arg_index + 1];
        old_column_value := (new_row ->> old_column_name)::timestamp;
        utc_column_value := (new_row ->> utc_column_name)::timestamptz;

        IF TG_OP = 'INSERT' THEN
            IF utc_column_value IS NOT NULL THEN
                patch := patch || jsonb_build_object(old_column_name, utc_column_value AT TIME ZONE 'UTC');
            ELSIF old_column_value IS NOT NULL THEN
                patch := patch || jsonb_build_object(utc_column_name, old_column_value AT TIME ZONE 'UTC');
            END IF;
        ELSE
            IF (new_row -> utc_column_name) IS DISTINCT FROM (old_row -> utc_column_name) THEN
                patch := patch || jsonb_build_object(old_column_name, utc_column_value AT TIME ZONE 'UTC');
            ELSIF (new_row -> old_column_name) IS DISTINCT FROM (old_row -> old_column_name) THEN
                patch := patch || jsonb_build_object(utc_column_name, old_column_value AT TIME ZONE 'UTC');
            END IF;
        END IF;

        arg_index := arg_index + 2;
    END LOOP;

    IF patch <> '{}'::jsonb THEN
        NEW := jsonb_populate_record(NEW, patch);
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create sync triggers before backfill so old app instances writing during migration also populate *_utc columns.
DROP TRIGGER IF EXISTS trigger_sync_attachments_utc_temporal_columns ON attachments;
CREATE TRIGGER trigger_sync_attachments_utc_temporal_columns
BEFORE INSERT OR UPDATE ON attachments
FOR EACH ROW
EXECUTE FUNCTION sync_utc_temporal_columns('created_at', 'created_at_utc', 'updated_at', 'updated_at_utc');

DROP TRIGGER IF EXISTS trigger_sync_categories_utc_temporal_columns ON categories;
CREATE TRIGGER trigger_sync_categories_utc_temporal_columns
BEFORE INSERT OR UPDATE ON categories
FOR EACH ROW
EXECUTE FUNCTION sync_utc_temporal_columns('created_at', 'created_at_utc', 'updated_at', 'updated_at_utc');

DROP TRIGGER IF EXISTS trigger_sync_comment_like_log_utc_temporal_columns ON comment_like_log;
CREATE TRIGGER trigger_sync_comment_like_log_utc_temporal_columns
BEFORE INSERT OR UPDATE ON comment_like_log
FOR EACH ROW
EXECUTE FUNCTION sync_utc_temporal_columns('created_at', 'created_at_utc', 'updated_at', 'updated_at_utc');

DROP TRIGGER IF EXISTS trigger_sync_comments_utc_temporal_columns ON comments;
CREATE TRIGGER trigger_sync_comments_utc_temporal_columns
BEFORE INSERT OR UPDATE ON comments
FOR EACH ROW
EXECUTE FUNCTION sync_utc_temporal_columns('created_at', 'created_at_utc', 'updated_at', 'updated_at_utc', 'deleted_at', 'deleted_at_utc');

DROP TRIGGER IF EXISTS trigger_sync_link_crawl_batches_utc_temporal_columns ON link_crawl_batches;
CREATE TRIGGER trigger_sync_link_crawl_batches_utc_temporal_columns
BEFORE INSERT OR UPDATE ON link_crawl_batches
FOR EACH ROW
EXECUTE FUNCTION sync_utc_temporal_columns('created_at', 'created_at_utc', 'updated_at', 'updated_at_utc', 'last_triggered_at', 'last_triggered_at_utc');

DROP TRIGGER IF EXISTS trigger_sync_link_daily_stats_utc_temporal_columns ON link_daily_stats;
CREATE TRIGGER trigger_sync_link_daily_stats_utc_temporal_columns
BEFORE INSERT OR UPDATE ON link_daily_stats
FOR EACH ROW
EXECUTE FUNCTION sync_utc_temporal_columns('created_at', 'created_at_utc', 'updated_at', 'updated_at_utc');

DROP TRIGGER IF EXISTS trigger_sync_link_like_log_utc_temporal_columns ON link_like_log;
CREATE TRIGGER trigger_sync_link_like_log_utc_temporal_columns
BEFORE INSERT OR UPDATE ON link_like_log
FOR EACH ROW
EXECUTE FUNCTION sync_utc_temporal_columns('created_at', 'created_at_utc', 'updated_at', 'updated_at_utc');

DROP TRIGGER IF EXISTS trigger_sync_link_read_log_utc_temporal_columns ON link_read_log;
CREATE TRIGGER trigger_sync_link_read_log_utc_temporal_columns
BEFORE INSERT OR UPDATE ON link_read_log
FOR EACH ROW
EXECUTE FUNCTION sync_utc_temporal_columns('created_at', 'created_at_utc', 'updated_at', 'updated_at_utc');

DROP TRIGGER IF EXISTS trigger_sync_link_view_log_utc_temporal_columns ON link_view_log;
CREATE TRIGGER trigger_sync_link_view_log_utc_temporal_columns
BEFORE INSERT OR UPDATE ON link_view_log
FOR EACH ROW
EXECUTE FUNCTION sync_utc_temporal_columns('created_at', 'created_at_utc', 'updated_at', 'updated_at_utc');

DROP TRIGGER IF EXISTS trigger_sync_links_utc_temporal_columns ON links;
CREATE TRIGGER trigger_sync_links_utc_temporal_columns
BEFORE INSERT OR UPDATE ON links
FOR EACH ROW
EXECUTE FUNCTION sync_utc_temporal_columns('created_at', 'created_at_utc', 'updated_at', 'updated_at_utc', 'published_at', 'published_at_utc');

DROP TRIGGER IF EXISTS trigger_sync_notification_arguments_utc_temporal_columns ON notification_arguments;
CREATE TRIGGER trigger_sync_notification_arguments_utc_temporal_columns
BEFORE INSERT OR UPDATE ON notification_arguments
FOR EACH ROW
EXECUTE FUNCTION sync_utc_temporal_columns('created_at', 'created_at_utc', 'updated_at', 'updated_at_utc');

DROP TRIGGER IF EXISTS trigger_sync_notification_recipients_utc_temporal_columns ON notification_recipients;
CREATE TRIGGER trigger_sync_notification_recipients_utc_temporal_columns
BEFORE INSERT OR UPDATE ON notification_recipients
FOR EACH ROW
EXECUTE FUNCTION sync_utc_temporal_columns('created_at', 'created_at_utc', 'updated_at', 'updated_at_utc', 'read_at', 'read_at_utc');

DROP TRIGGER IF EXISTS trigger_sync_notifications_utc_temporal_columns ON notifications;
CREATE TRIGGER trigger_sync_notifications_utc_temporal_columns
BEFORE INSERT OR UPDATE ON notifications
FOR EACH ROW
EXECUTE FUNCTION sync_utc_temporal_columns('created_at', 'created_at_utc', 'updated_at', 'updated_at_utc');

DROP TRIGGER IF EXISTS trigger_sync_post_daily_stats_utc_temporal_columns ON post_daily_stats;
CREATE TRIGGER trigger_sync_post_daily_stats_utc_temporal_columns
BEFORE INSERT OR UPDATE ON post_daily_stats
FOR EACH ROW
EXECUTE FUNCTION sync_utc_temporal_columns('created_at', 'created_at_utc', 'updated_at', 'updated_at_utc');

DROP TRIGGER IF EXISTS trigger_sync_post_like_log_utc_temporal_columns ON post_like_log;
CREATE TRIGGER trigger_sync_post_like_log_utc_temporal_columns
BEFORE INSERT OR UPDATE ON post_like_log
FOR EACH ROW
EXECUTE FUNCTION sync_utc_temporal_columns('created_at', 'created_at_utc', 'updated_at', 'updated_at_utc');

DROP TRIGGER IF EXISTS trigger_sync_post_read_log_utc_temporal_columns ON post_read_log;
CREATE TRIGGER trigger_sync_post_read_log_utc_temporal_columns
BEFORE INSERT OR UPDATE ON post_read_log
FOR EACH ROW
EXECUTE FUNCTION sync_utc_temporal_columns('created_at', 'created_at_utc', 'updated_at', 'updated_at_utc');

DROP TRIGGER IF EXISTS trigger_sync_post_view_log_utc_temporal_columns ON post_view_log;
CREATE TRIGGER trigger_sync_post_view_log_utc_temporal_columns
BEFORE INSERT OR UPDATE ON post_view_log
FOR EACH ROW
EXECUTE FUNCTION sync_utc_temporal_columns('created_at', 'created_at_utc', 'updated_at', 'updated_at_utc');

DROP TRIGGER IF EXISTS trigger_sync_posts_utc_temporal_columns ON posts;
CREATE TRIGGER trigger_sync_posts_utc_temporal_columns
BEFORE INSERT OR UPDATE ON posts
FOR EACH ROW
EXECUTE FUNCTION sync_utc_temporal_columns('created_at', 'created_at_utc', 'updated_at', 'updated_at_utc', 'stats_updated_at', 'stats_updated_at_utc');

DROP TRIGGER IF EXISTS trigger_sync_tags_utc_temporal_columns ON tags;
CREATE TRIGGER trigger_sync_tags_utc_temporal_columns
BEFORE INSERT OR UPDATE ON tags
FOR EACH ROW
EXECUTE FUNCTION sync_utc_temporal_columns('created_at', 'created_at_utc', 'updated_at', 'updated_at_utc');

DROP TRIGGER IF EXISTS trigger_sync_user_bans_utc_temporal_columns ON user_bans;
CREATE TRIGGER trigger_sync_user_bans_utc_temporal_columns
BEFORE INSERT OR UPDATE ON user_bans
FOR EACH ROW
EXECUTE FUNCTION sync_utc_temporal_columns('created_at', 'created_at_utc', 'updated_at', 'updated_at_utc');

DROP TRIGGER IF EXISTS trigger_sync_user_follows_utc_temporal_columns ON user_follows;
CREATE TRIGGER trigger_sync_user_follows_utc_temporal_columns
BEFORE INSERT OR UPDATE ON user_follows
FOR EACH ROW
EXECUTE FUNCTION sync_utc_temporal_columns('created_at', 'created_at_utc', 'updated_at', 'updated_at_utc');

DROP TRIGGER IF EXISTS trigger_sync_user_links_utc_temporal_columns ON user_links;
CREATE TRIGGER trigger_sync_user_links_utc_temporal_columns
BEFORE INSERT OR UPDATE ON user_links
FOR EACH ROW
EXECUTE FUNCTION sync_utc_temporal_columns('created_at', 'created_at_utc', 'updated_at', 'updated_at_utc');

DROP TRIGGER IF EXISTS trigger_sync_user_tokens_utc_temporal_columns ON user_tokens;
CREATE TRIGGER trigger_sync_user_tokens_utc_temporal_columns
BEFORE INSERT OR UPDATE ON user_tokens
FOR EACH ROW
EXECUTE FUNCTION sync_utc_temporal_columns('created_at', 'created_at_utc', 'updated_at', 'updated_at_utc');

DROP TRIGGER IF EXISTS trigger_sync_users_utc_temporal_columns ON users;
CREATE TRIGGER trigger_sync_users_utc_temporal_columns
BEFORE INSERT OR UPDATE ON users
FOR EACH ROW
EXECUTE FUNCTION sync_utc_temporal_columns('created_at', 'created_at_utc', 'updated_at', 'updated_at_utc');

-- Backfill old timestamp values as UTC instants. Existing naive TIMESTAMP values are interpreted as UTC.
UPDATE attachments SET created_at_utc = created_at AT TIME ZONE 'UTC' WHERE created_at IS NOT NULL AND created_at_utc IS NULL;
UPDATE attachments SET updated_at_utc = updated_at AT TIME ZONE 'UTC' WHERE updated_at IS NOT NULL AND updated_at_utc IS NULL;
UPDATE categories SET created_at_utc = created_at AT TIME ZONE 'UTC' WHERE created_at IS NOT NULL AND created_at_utc IS NULL;
UPDATE categories SET updated_at_utc = updated_at AT TIME ZONE 'UTC' WHERE updated_at IS NOT NULL AND updated_at_utc IS NULL;
UPDATE comment_like_log SET created_at_utc = created_at AT TIME ZONE 'UTC' WHERE created_at IS NOT NULL AND created_at_utc IS NULL;
UPDATE comment_like_log SET updated_at_utc = updated_at AT TIME ZONE 'UTC' WHERE updated_at IS NOT NULL AND updated_at_utc IS NULL;
UPDATE comments SET created_at_utc = created_at AT TIME ZONE 'UTC' WHERE created_at IS NOT NULL AND created_at_utc IS NULL;
UPDATE comments SET updated_at_utc = updated_at AT TIME ZONE 'UTC' WHERE updated_at IS NOT NULL AND updated_at_utc IS NULL;
UPDATE link_crawl_batches SET created_at_utc = created_at AT TIME ZONE 'UTC' WHERE created_at IS NOT NULL AND created_at_utc IS NULL;
UPDATE link_crawl_batches SET updated_at_utc = updated_at AT TIME ZONE 'UTC' WHERE updated_at IS NOT NULL AND updated_at_utc IS NULL;
UPDATE link_daily_stats SET created_at_utc = created_at AT TIME ZONE 'UTC' WHERE created_at IS NOT NULL AND created_at_utc IS NULL;
UPDATE link_daily_stats SET updated_at_utc = updated_at AT TIME ZONE 'UTC' WHERE updated_at IS NOT NULL AND updated_at_utc IS NULL;
UPDATE link_like_log SET created_at_utc = created_at AT TIME ZONE 'UTC' WHERE created_at IS NOT NULL AND created_at_utc IS NULL;
UPDATE link_like_log SET updated_at_utc = updated_at AT TIME ZONE 'UTC' WHERE updated_at IS NOT NULL AND updated_at_utc IS NULL;
UPDATE link_read_log SET created_at_utc = created_at AT TIME ZONE 'UTC' WHERE created_at IS NOT NULL AND created_at_utc IS NULL;
UPDATE link_read_log SET updated_at_utc = updated_at AT TIME ZONE 'UTC' WHERE updated_at IS NOT NULL AND updated_at_utc IS NULL;
UPDATE link_view_log SET created_at_utc = created_at AT TIME ZONE 'UTC' WHERE created_at IS NOT NULL AND created_at_utc IS NULL;
UPDATE link_view_log SET updated_at_utc = updated_at AT TIME ZONE 'UTC' WHERE updated_at IS NOT NULL AND updated_at_utc IS NULL;
UPDATE links SET created_at_utc = created_at AT TIME ZONE 'UTC' WHERE created_at IS NOT NULL AND created_at_utc IS NULL;
UPDATE links SET updated_at_utc = updated_at AT TIME ZONE 'UTC' WHERE updated_at IS NOT NULL AND updated_at_utc IS NULL;
UPDATE notification_arguments SET created_at_utc = created_at AT TIME ZONE 'UTC' WHERE created_at IS NOT NULL AND created_at_utc IS NULL;
UPDATE notification_arguments SET updated_at_utc = updated_at AT TIME ZONE 'UTC' WHERE updated_at IS NOT NULL AND updated_at_utc IS NULL;
UPDATE notification_recipients SET created_at_utc = created_at AT TIME ZONE 'UTC' WHERE created_at IS NOT NULL AND created_at_utc IS NULL;
UPDATE notification_recipients SET updated_at_utc = updated_at AT TIME ZONE 'UTC' WHERE updated_at IS NOT NULL AND updated_at_utc IS NULL;
UPDATE notifications SET created_at_utc = created_at AT TIME ZONE 'UTC' WHERE created_at IS NOT NULL AND created_at_utc IS NULL;
UPDATE notifications SET updated_at_utc = updated_at AT TIME ZONE 'UTC' WHERE updated_at IS NOT NULL AND updated_at_utc IS NULL;
UPDATE post_daily_stats SET created_at_utc = created_at AT TIME ZONE 'UTC' WHERE created_at IS NOT NULL AND created_at_utc IS NULL;
UPDATE post_daily_stats SET updated_at_utc = updated_at AT TIME ZONE 'UTC' WHERE updated_at IS NOT NULL AND updated_at_utc IS NULL;
UPDATE post_like_log SET created_at_utc = created_at AT TIME ZONE 'UTC' WHERE created_at IS NOT NULL AND created_at_utc IS NULL;
UPDATE post_like_log SET updated_at_utc = updated_at AT TIME ZONE 'UTC' WHERE updated_at IS NOT NULL AND updated_at_utc IS NULL;
UPDATE post_read_log SET created_at_utc = created_at AT TIME ZONE 'UTC' WHERE created_at IS NOT NULL AND created_at_utc IS NULL;
UPDATE post_read_log SET updated_at_utc = updated_at AT TIME ZONE 'UTC' WHERE updated_at IS NOT NULL AND updated_at_utc IS NULL;
UPDATE post_view_log SET created_at_utc = created_at AT TIME ZONE 'UTC' WHERE created_at IS NOT NULL AND created_at_utc IS NULL;
UPDATE post_view_log SET updated_at_utc = updated_at AT TIME ZONE 'UTC' WHERE updated_at IS NOT NULL AND updated_at_utc IS NULL;
UPDATE posts SET created_at_utc = created_at AT TIME ZONE 'UTC' WHERE created_at IS NOT NULL AND created_at_utc IS NULL;
UPDATE posts SET updated_at_utc = updated_at AT TIME ZONE 'UTC' WHERE updated_at IS NOT NULL AND updated_at_utc IS NULL;
UPDATE tags SET created_at_utc = created_at AT TIME ZONE 'UTC' WHERE created_at IS NOT NULL AND created_at_utc IS NULL;
UPDATE tags SET updated_at_utc = updated_at AT TIME ZONE 'UTC' WHERE updated_at IS NOT NULL AND updated_at_utc IS NULL;
UPDATE user_bans SET created_at_utc = created_at AT TIME ZONE 'UTC' WHERE created_at IS NOT NULL AND created_at_utc IS NULL;
UPDATE user_bans SET updated_at_utc = updated_at AT TIME ZONE 'UTC' WHERE updated_at IS NOT NULL AND updated_at_utc IS NULL;
UPDATE user_follows SET created_at_utc = created_at AT TIME ZONE 'UTC' WHERE created_at IS NOT NULL AND created_at_utc IS NULL;
UPDATE user_follows SET updated_at_utc = updated_at AT TIME ZONE 'UTC' WHERE updated_at IS NOT NULL AND updated_at_utc IS NULL;
UPDATE user_links SET created_at_utc = created_at AT TIME ZONE 'UTC' WHERE created_at IS NOT NULL AND created_at_utc IS NULL;
UPDATE user_links SET updated_at_utc = updated_at AT TIME ZONE 'UTC' WHERE updated_at IS NOT NULL AND updated_at_utc IS NULL;
UPDATE user_tokens SET created_at_utc = created_at AT TIME ZONE 'UTC' WHERE created_at IS NOT NULL AND created_at_utc IS NULL;
UPDATE user_tokens SET updated_at_utc = updated_at AT TIME ZONE 'UTC' WHERE updated_at IS NOT NULL AND updated_at_utc IS NULL;
UPDATE users SET created_at_utc = created_at AT TIME ZONE 'UTC' WHERE created_at IS NOT NULL AND created_at_utc IS NULL;
UPDATE users SET updated_at_utc = updated_at AT TIME ZONE 'UTC' WHERE updated_at IS NOT NULL AND updated_at_utc IS NULL;
UPDATE comments SET deleted_at_utc = deleted_at AT TIME ZONE 'UTC' WHERE deleted_at IS NOT NULL AND deleted_at_utc IS NULL;
UPDATE link_crawl_batches SET last_triggered_at_utc = last_triggered_at AT TIME ZONE 'UTC' WHERE last_triggered_at IS NOT NULL AND last_triggered_at_utc IS NULL;
UPDATE links SET published_at_utc = published_at AT TIME ZONE 'UTC' WHERE published_at IS NOT NULL AND published_at_utc IS NULL;
UPDATE notification_recipients SET read_at_utc = read_at AT TIME ZONE 'UTC' WHERE read_at IS NOT NULL AND read_at_utc IS NULL;
UPDATE posts SET stats_updated_at_utc = stats_updated_at AT TIME ZONE 'UTC' WHERE stats_updated_at IS NOT NULL AND stats_updated_at_utc IS NULL;
