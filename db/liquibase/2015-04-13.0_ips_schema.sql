-- liquibase formatted SQL
-- changeset andy:17

-- Introduce a concept of a generic page.
RENAME TABLE `questions` TO `pages`;

-- Introduce a way to distinguish page types.
ALTER TABLE `pages` ADD `type` VARCHAR(15) NOT NULL;
UPDATE `pages` SET `type` = 'question';

-- Fields specific for external links.
ALTER TABLE `pages` ADD `ext_link_name` VARCHAR(70) NULL;
ALTER TABLE `pages` ADD `ext_link_url` VARCHAR(255) NULL;

-- A long-desired cleanup: the whole `active'/`inactive' naming seems ambiguous.
ALTER TABLE `pages` CHANGE `active` `deleted` BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE `pages` SET `deleted` = NOT `deleted`;

-- A long-desired cleanup: now we don't even provide a way to rearrange questions from the UI,
-- thus making the whole `order' field mess just redundant.
ALTER TABLE `pages` DROP `question_order`;