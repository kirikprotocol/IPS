-- liquibase formatted SQL

-- changeset andy:1

--
-- Table structure for table `surveys`
--

CREATE TABLE `surveys` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `expires` datetime NOT NULL,
  `startdate` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=31484 DEFAULT CHARSET=utf8;

--
-- Table structure for table `surveys_text`
--

CREATE TABLE `surveys_text` (
  `description` text,
  `end_text` text,
  `title` varchar(200) DEFAULT NULL,
  `welcome_text` text,
  `survey_id` int(11) NOT NULL,
  PRIMARY KEY (`survey_id`),
  CONSTRAINT `FK_surveys_text_surveys` FOREIGN KEY (`survey_id`) REFERENCES `surveys` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure for table `survey_stats`
--

CREATE TABLE `survey_stats` (
  `accessNumber` varchar(255) DEFAULT NULL,
  `answeredCount` int(11) NOT NULL DEFAULT '0',
  `campaign` varchar(255) DEFAULT NULL,
  `channel` varchar(255) NOT NULL DEFAULT 'CLIENT_BASE',
  `registeredCount` int(11) NOT NULL DEFAULT '0',
  `sent` int(11) NOT NULL DEFAULT '0',
  `survey_id` int(11) NOT NULL,
  PRIMARY KEY (`survey_id`),
  CONSTRAINT `FK_survey_stats_surveys` FOREIGN KEY (`survey_id`) REFERENCES `surveys` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure for table `questions`
--

CREATE TABLE `questions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `title` varchar(255) NOT NULL,
  `survey_id` int(11) NOT NULL,
  `question_order` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_questions_surveys` (`survey_id`),
  CONSTRAINT `FK_questions_surveys` FOREIGN KEY (`survey_id`) REFERENCES `surveys` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8;

--
-- Table structure for table `question_options`
--

CREATE TABLE `question_options` (
  `question_id` int(11) NOT NULL,
  `answer` text NOT NULL,
  `option_order` int(11) NOT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`),
  KEY `FK_question_options_questions` (`question_id`),
  CONSTRAINT `FK_question_options_questions` FOREIGN KEY (`question_id`) REFERENCES `questions` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=163 DEFAULT CHARSET=utf8;

--
-- Table structure for table `question_stats`
--

CREATE TABLE `question_stats` (
  `question_id` int(11) NOT NULL,
  `answerCount` int(11) DEFAULT NULL,
  `resentCount` int(11) DEFAULT NULL,
  `sentCount` int(11) DEFAULT NULL,
  PRIMARY KEY (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure for table `respondents`
--

CREATE TABLE `respondents` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `answered` tinyint(1) NOT NULL DEFAULT '0',
  `MSISDN` varchar(255) NOT NULL,
  `survey_id` int(11) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8;

--
-- Table structure for table `answers`
--

CREATE TABLE `answers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `respondent_id` int(11) NOT NULL,
  `question_id` int(11) NOT NULL,
  `option_id` int(11) NOT NULL,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `FK_answers_respondents` (`respondent_id`),
  KEY `FK_answers_questions` (`question_id`),
  KEY `FK_answers_question_options` (`option_id`),
  CONSTRAINT `FK_answers_respondents` FOREIGN KEY (`respondent_id`) REFERENCES `respondents` (`id`),
  CONSTRAINT `FK_answers_questions` FOREIGN KEY (`question_id`) REFERENCES `questions` (`id`),
  CONSTRAINT `FK_answers_question_options` FOREIGN KEY (`option_id`) REFERENCES `question_options` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=33 DEFAULT CHARSET=utf8;

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `email` varchar(255) NOT NULL,
  `full_name` varchar(255) DEFAULT NULL,
  `users_name` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `blocked` tinyint(1) NOT NULL DEFAULT '0',
  `role` varchar(255) NOT NULL,
  `company` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `users_name` (`users_name`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=112 DEFAULT CHARSET=utf8;

--
-- Table structure for table `surveys_users`
--

CREATE TABLE `surveys_users` (
  `survey_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  PRIMARY KEY (`survey_id`,`user_id`),
  KEY `FK_surveys_users_users` (`user_id`),
  CONSTRAINT `FK_surveys_users_surveys` FOREIGN KEY (`survey_id`) REFERENCES `surveys` (`id`),
  CONSTRAINT `FK_surveys_users_users` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
