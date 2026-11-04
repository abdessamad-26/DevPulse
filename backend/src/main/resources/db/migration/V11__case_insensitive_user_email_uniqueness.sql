-- Keep the existing users table intact while preventing duplicate accounts
-- whose email addresses differ only by letter case.
CREATE UNIQUE INDEX uq_users_email_lower ON users (LOWER(email));
