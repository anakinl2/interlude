#!/bin/bash

DBHOST=localhost
USER=root
PASS=
DBNAME=L2Dreamdb

while :;
do
	#mysqlcheck -h $DBHOST -u $USER --password=$PASS -s -r $DBNAME>>"log/`date +%Y-%m-%d_%H:%M:%S`-sql_check.log"
	#mysqldump -h $DBHOST -u $USER --password=$PASS $DBNAME | gzip > "backup/`date +%Y-%m-%d_%H:%M:%S`-"$DBNAME"_gameserver.gz"
	mv log/java0.log.0 "log/`date +%Y-%m-%d_%H-%M-%S`_java.log"
	mv log/stdout.log "log/`date +%Y-%m-%d_%H-%M-%S`_stdout.log"
	mv log/chat.log "log/`date +%Y-%m-%d_%H:%M:%S`-chat.log"
	nice -n -2 java -Dfile.encoding=UTF-8 -Xms1024m -Xmx1024m -cp lib/*: com.lineage.game.GameServer > log/stdout.log 2>&1
	[ $? -ne 2 ] && break
	sleep 10;
done

