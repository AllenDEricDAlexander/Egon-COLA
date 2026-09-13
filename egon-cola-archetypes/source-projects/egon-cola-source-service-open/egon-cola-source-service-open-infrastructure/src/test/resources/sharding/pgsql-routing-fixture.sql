-- Run only inside the integration test's newly created, disposable schema.
CREATE TABLE routing_probe (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    create_user_id VARCHAR(64) NOT NULL,
    update_user_id VARCHAR(64) NOT NULL,
    create_time TIMESTAMPTZ NOT NULL,
    update_time TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMP NULL,
    version BIGINT NOT NULL DEFAULT 0,
    order_id BIGINT NOT NULL,
    payload VARCHAR(64) NOT NULL
);
INSERT INTO routing_probe VALUES
(1, 41, 'test', 'test', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0, 101, 'active-a'),
(2, 42, 'test', 'test', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 1, 102, 'active-b'),
(3, 41, 'test', 'test', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP AT TIME ZONE 'UTC', 2, 101, 'deleted-a');

CREATE TABLE routing_order_t0_b0 (LIKE routing_probe INCLUDING ALL);
CREATE TABLE routing_order_item_t0_b0 (LIKE routing_probe INCLUDING ALL);
CREATE TABLE routing_order_t0_b1 (LIKE routing_probe INCLUDING ALL);
CREATE TABLE routing_order_item_t0_b1 (LIKE routing_probe INCLUDING ALL);
CREATE TABLE routing_order_t1_b0 (LIKE routing_probe INCLUDING ALL);
CREATE TABLE routing_order_item_t1_b0 (LIKE routing_probe INCLUDING ALL);
CREATE TABLE routing_order_t1_b1 (LIKE routing_probe INCLUDING ALL);
CREATE TABLE routing_order_item_t1_b1 (LIKE routing_probe INCLUDING ALL);
CREATE TABLE routing_order_t2_b0 (LIKE routing_probe INCLUDING ALL);
CREATE TABLE routing_order_item_t2_b0 (LIKE routing_probe INCLUDING ALL);
CREATE TABLE routing_order_t2_b1 (LIKE routing_probe INCLUDING ALL);
CREATE TABLE routing_order_item_t2_b1 (LIKE routing_probe INCLUDING ALL);
CREATE TABLE routing_order_t3_b0 (LIKE routing_probe INCLUDING ALL);
CREATE TABLE routing_order_item_t3_b0 (LIKE routing_probe INCLUDING ALL);
CREATE TABLE routing_order_t3_b1 (LIKE routing_probe INCLUDING ALL);
CREATE TABLE routing_order_item_t3_b1 (LIKE routing_probe INCLUDING ALL);
CREATE TABLE routing_metadata (LIKE routing_probe INCLUDING ALL);
CREATE TABLE routing_dictionary (LIKE routing_probe INCLUDING ALL);
INSERT INTO routing_metadata SELECT * FROM routing_probe WHERE id = 1;
INSERT INTO routing_dictionary SELECT * FROM routing_probe WHERE id = 1;
