-- MySQL Script generated by MySQL Workbench
-- Do 21 Sep 2017 23:18:41 CEST
-- Model: New Model    Version: 1.0
-- MySQL Workbench Forward Engineering

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL,ALLOW_INVALID_DATES';

-- -----------------------------------------------------
-- Schema SecVM_DB
-- -----------------------------------------------------

-- -----------------------------------------------------
-- Schema SecVM_DB
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `SecVM_DB` DEFAULT CHARACTER SET utf8 ;
USE `SecVM_DB` ;

-- -----------------------------------------------------
-- Table `SecVM_DB`.`dice_roll`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `SecVM_DB`.`dice_roll` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `probabilities` TEXT NOT NULL,
  PRIMARY KEY (`id`))
ENGINE = InnoDB
COMMENT = 'probabilities = Base64 encoded float array';


-- -----------------------------------------------------
-- Table `SecVM_DB`.`svm`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `SecVM_DB`.`svm` (
  `svm_id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` TEXT NULL,
  `lambda` FLOAT NOT NULL,
  `num_bins` INT UNSIGNED NOT NULL,
  `min_number_train_participants` INT UNSIGNED NOT NULL,
  `dice_roll_id` INT UNSIGNED NOT NULL,
  `train_outcomes_dice_roll` TEXT NOT NULL,
  `test_outcomes_dice_roll` TEXT NOT NULL,
  `train_enabled` TINYINT(1) UNSIGNED NOT NULL,
  `test_enabled` TINYINT(1) UNSIGNED NOT NULL,
  PRIMARY KEY (`svm_id`),
  INDEX `fk_svm_dice_roll1_idx` (`dice_roll_id` ASC),
  CONSTRAINT `fk_svm_dice_roll1`
    FOREIGN KEY (`dice_roll_id`)
    REFERENCES `SecVM_DB`.`dice_roll` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB
COMMENT = 'svm_id: AUTOINCREMENT\ntrain_outcomes_dice_roll, test_outcomes_dice_roll: Base64 encoded int array\n\nmay be manually change:\nenabled';


-- -----------------------------------------------------
-- Table `SecVM_DB`.`weight_vector`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `SecVM_DB`.`weight_vector` (
  `svm_id` INT UNSIGNED NOT NULL,
  `iteration` INT UNSIGNED NOT NULL,
  `training_start_time` TIMESTAMP(3) NOT NULL,
  `training_end_time` TIMESTAMP(3) NULL,
  `num_participants` INT UNSIGNED NOT NULL,
  `weights` TEXT NOT NULL,
  PRIMARY KEY (`svm_id`, `iteration`),
  INDEX `fk_weight_vector_smv_idx` (`svm_id` ASC),
  CONSTRAINT `fk_weight_vector_smv`
    FOREIGN KEY (`svm_id`)
    REFERENCES `SecVM_DB`.`svm` (`svm_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB
COMMENT = 'weights: Base64 encoded';


-- -----------------------------------------------------
-- Table `SecVM_DB`.`package_participation`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `SecVM_DB`.`package_participation` (
  `autoincrement_id` BIGINT UNSIGNED NOT NULL,
  `svm_id` INT UNSIGNED NOT NULL,
  `iteration` INT UNSIGNED NOT NULL,
  `package_random_id` TEXT NOT NULL,
  `timestamp` TIMESTAMP(3) NOT NULL,
  PRIMARY KEY (`autoincrement_id`),
  CONSTRAINT `fk_package_participation_weight_vector1`
    FOREIGN KEY (`svm_id` , `iteration`)
    REFERENCES `SecVM_DB`.`weight_vector` (`svm_id` , `iteration`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `SecVM_DB`.`test_accuracy`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `SecVM_DB`.`test_accuracy` (
  `svm_id` INT UNSIGNED NOT NULL,
  `iteration` INT UNSIGNED NOT NULL,
  `female_overall` INT UNSIGNED NOT NULL,
  `male_overall` INT UNSIGNED NOT NULL,
  `female_correct` INT UNSIGNED NOT NULL,
  `male_correct` INT UNSIGNED NOT NULL,
  PRIMARY KEY (`svm_id`, `iteration`),
  CONSTRAINT `fk_test_accuracy_weight_vector1`
    FOREIGN KEY (`svm_id` , `iteration`)
    REFERENCES `SecVM_DB`.`weight_vector` (`svm_id` , `iteration`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB
COMMENT = 'LONGBLOB = ARRAY(FLOAT)';


-- -----------------------------------------------------
-- Table `SecVM_DB`.`feature_type`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `SecVM_DB`.`feature_type` (
  `name` VARCHAR(10) NOT NULL,
  PRIMARY KEY (`name`))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `SecVM_DB`.`feature_vector`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `SecVM_DB`.`feature_vector` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `feature_type` VARCHAR(10) NOT NULL,
  `num_features` INT UNSIGNED NOT NULL,
  `num_hashes` INT UNSIGNED NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `fk_feature_vector_feature_type1_idx` (`feature_type` ASC),
  CONSTRAINT `fk_feature_vector_feature_type1`
    FOREIGN KEY (`feature_type`)
    REFERENCES `SecVM_DB`.`feature_type` (`name`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB
COMMENT = 'feature_id: autoincrement';


-- -----------------------------------------------------
-- Table `SecVM_DB`.`package_train`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `SecVM_DB`.`package_train` (
  `autoincrement_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `svm_id` INT UNSIGNED NOT NULL,
  `iteration` INT UNSIGNED NOT NULL,
  `package_random_id` TEXT NOT NULL,
  `timestamp` TIMESTAMP(3) NOT NULL,
  `index` INT NOT NULL,
  `value` TINYINT(1) NOT NULL,
  PRIMARY KEY (`autoincrement_id`),
  CONSTRAINT `fk_package_participation_weight_vector10`
    FOREIGN KEY (`svm_id` , `iteration`)
    REFERENCES `SecVM_DB`.`weight_vector` (`svm_id` , `iteration`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `SecVM_DB`.`package_test`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `SecVM_DB`.`package_test` (
  `autoincrement_id` BIGINT UNSIGNED NOT NULL,
  `svm_id` INT UNSIGNED NOT NULL,
  `iteration` INT UNSIGNED NOT NULL,
  `package_random_id` TEXT NOT NULL,
  `timestamp` TIMESTAMP(3) NOT NULL,
  `true_gender` TINYINT(1) NOT NULL,
  `predicted_gender` TINYINT(1) NOT NULL,
  PRIMARY KEY (`autoincrement_id`),
  CONSTRAINT `fk_package_test_test_accuracy1`
    FOREIGN KEY (`svm_id` , `iteration`)
    REFERENCES `SecVM_DB`.`test_accuracy` (`svm_id` , `iteration`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `SecVM_DB`.`svm_uses_feature_vector`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `SecVM_DB`.`svm_uses_feature_vector` (
  `svm_id` INT UNSIGNED NOT NULL,
  `feature_vector_id` INT UNSIGNED NOT NULL,
  PRIMARY KEY (`svm_id`, `feature_vector_id`),
  INDEX `fk_svm_has_feature_vector_feature_vector2_idx` (`feature_vector_id` ASC),
  INDEX `fk_svm_has_feature_vector_svm2_idx` (`svm_id` ASC),
  CONSTRAINT `fk_svm_has_feature_vector_svm2`
    FOREIGN KEY (`svm_id`)
    REFERENCES `SecVM_DB`.`svm` (`svm_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_svm_has_feature_vector_feature_vector2`
    FOREIGN KEY (`feature_vector_id`)
    REFERENCES `SecVM_DB`.`feature_vector` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
