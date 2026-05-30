UPDATE users
SET
    role = 'ADMIN',
    updated_at = CURRENT_TIMESTAMP
WHERE email = 'rookedsysc36@gmail.com'
  AND role <> 'ADMIN';
