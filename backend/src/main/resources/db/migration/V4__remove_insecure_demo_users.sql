-- Remove fixed-password demo accounts introduced by the development seed.
-- Production operators must be provisioned explicitly through secure deployment configuration.
DELETE FROM human_annotations
WHERE annotator_id IN ('user-admin-001', 'user-operator-001');

DELETE FROM refresh_tokens
WHERE user_id IN ('user-admin-001', 'user-operator-001');

DELETE FROM users
WHERE id IN ('user-admin-001', 'user-operator-001');
