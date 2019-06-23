package com.datastax.kellett;

import java.util.Scanner;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

public class Lab4 {
    public static void main(String[] args) {

        // Connect to Spark
        System.out.println("About to build session...");
        SparkSession spark = SparkSession.builder()
                .appName("Spark-SQL Lab 4")           // any name you like... will display in Spark Management UI
                .enableHiveSupport()    // Enables connection to a Hive metastore, support for hive SerDes and Hive user-defined functions
                .getOrCreate(); // gets existing spark session if present, or creates a new one

        // We'll read in a .csv file into a Spark-SQL object that can be treated just like a database table.
        // In the real world, this could be a flat file or perhaps a database table.
        // NOTE that we placed this file on the DSEFS file system to make it accessible to all Spark nodes
        System.out.println("About to read transactions_buy.csv...");
        Dataset<Row> incoming_buy_transactions = spark.read().option("header", true).csv("analytics_workshop.transactions_buy.csv");

        // Now let's turn it into a View so we can use SQL against it.
        incoming_buy_transactions.createOrReplaceTempView("new_buys");

        // Next, let's whittle the table down so it only shows large transactions.
        // Notice that we are now working in pure SQL.
        Dataset<Row> newBuys = spark.sql("SELECT * FROM new_buys WHERE units > 999");

        // The new_buys table looks good.  Now let's do all the same things and make a new_sells table.
        // NOTE that we placed this file on the DSEFS file system to make it accessible to all Spark nodes
        System.out.println("About to read transactions_sell.csv...");
        Dataset<Row> incoming_sell_transactions = spark.read().option("header", true).csv("analytics_workshop.transactions_sell.csv");
        incoming_sell_transactions.createOrReplaceTempView("new_sells");
        Dataset<Row> newSells = spark.sql("SELECT * FROM new_sells WHERE units > 999");

        // Here is a relatively complex query that Joins the (flat file) transaction_buy table with the (Cassandra) customers table,
        // and then Unions it with a Join between the (flat file) transaction_sell table and the (Cassandra) customers table,
        // and then inserts the entire result into the (Cassandra) transactions_historical table.
        // IT ALL ACTS AS A SINGLE DATABASE!!!
        System.out.println("About to perform SQL INSERT operation...");
        Dataset<Row> ds = spark.sql("INSERT INTO analytics_workshop.transactions_historical "
                + "SELECT "
                + "c.account_number        AS account_number, "
                + "t.transaction_id        AS transaction_id, "
                + "c.city                  AS account_city, "
                + "c.country               AS account_country, "
                + "c.first_name            AS account_first_name, "
                + "c.last_name             AS account_last_name, "
                + "c.gender                AS account_gender, "
                + "t.buy_or_sell           AS buy_or_sell, "
                + "t.industry_sector       AS industry_sector, "
                + "t.instrument_id         AS instrument_id, "
                + "t.instrument_industry   AS instrument_industry, "
                + "t.instrument_name       AS instrument_name, "
                + "t.transaction_date      AS transaction_date, "
                + "t.transaction_time      AS transaction_time, "
                + "t.units                 AS units "

                + "FROM new_buys t INNER JOIN analytics_workshop.customers c "
                + "ON t.account_number = c.account_number "
                + "WHERE t.units > 999 "

                + "UNION "

                + "SELECT "
                + "c.account_number        AS account_number, "
                + "t.transaction_id        AS transaction_id, "
                + "c.city                  AS account_city, "
                + "c.country               AS account_country, "
                + "c.first_name            AS account_first_name, "
                + "c.last_name             AS account_last_name, "
                + "c.gender                AS account_gender, "
                + "t.buy_or_sell           AS buy_or_sell, "
                + "t.industry_sector       AS industry_sector, "
                + "t.instrument_id         AS instrument_id, "
                + "t.instrument_industry   AS instrument_industry, "
                + "t.instrument_name       AS instrument_name, "
                + "t.transaction_date      AS transaction_date, "
                + "t.transaction_time      AS transaction_time, "
                + "t.units                 AS units "

                + "FROM new_sells t INNER JOIN analytics_workshop.customers c "
                + "ON t.account_number = c.account_number "
                + "WHERE t.units > 999");

        System.out.println("... SQL INSERT operation complete.");

        System.out.println("Waiting here to allow time for viewing Spark UI on port 7080.");
        System.out.println("Press enter when you want to terminate.");
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();

        spark.close();
    }
}
