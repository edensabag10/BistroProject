***After cloning the project, you must re-add the MySQL Connector/J JAR manually (the driver is not included in the repository).***

How to Run the Project and Initialize the Database
1. Create the database schema

  Inside the project you will find the DDL file at:
  Server/data_base/ProjectDB.sql

  This file contains the SQL script used to create the database schema required by the system.

   How to execute it:
    *Open MySQL Workbench
    *Go to File → Open SQL Script and select the file - ProjectDB.sql
    *Click the "Run All" (lightning bolt) button
    *The database and all required tables will be created automatically

2. Update your DB connection settings

  Before running the server, update the MySQL credentials inside 

  Server->src->server->DBController.java

  The code:
   conn = DriverManager.getConnection(
       "jdbc:mysql://localhost:3307/bistrodb?serverTimezone=Asia/Jerusalem&useSSL=false",
       "root",
       "YOUR_PASSWORD"
   );
