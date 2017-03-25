1-create new project in your IDE, add files in src folder to it
2-add the jars in the libraries folder to your dependencies in the IDE
3-create database with name search_engine, use database
4-run the database script found in setup folder

To Run the Crawler: 
1-in file DB.java change the user and pass variables to your database user name and password
2-run CrawlerMain.java, it's currently set to 10 threads and 6000 documents to download.

=======================================================

To Run the indexer:
1- first set configurations settings in your id as in the eclipse settings image in settings folder
-open run
-run configurations
-open arguments
-write those lines:
	-Xms512M -Xmx4G
	-XX:+UseConcMarkSweepGC

2-Set mysql setting
check the mysql_settings image and make sure the settings are the same as in the picture

3-Creating database
-to import 6000docs to index them:
	source (path)/6000docs.sql;
	it might give an error if there is a "\" in path
	example:
	this is correct: D:/java/project/6000docs.sql
	this isnt:  D:\java\project\6000docs.sql

4- Run in Eclipse
- open Indexer.java and make sure stopwords path in line 39 is the same as your own
- make sure the DB class variables (user name and password) are as your own
- Run IndexerMain.java, it should take about an hour and half
