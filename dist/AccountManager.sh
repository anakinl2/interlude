#!/bin/sh
java -Djava.util.logging.config.file=config/console.cfg -cp lib/*; l2d.accountmanager.SQLAccountManager
