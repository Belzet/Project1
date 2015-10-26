import java.util.*;
import java.sql.*;

public class Application{

  public String client_email = "";
  public String client_password = "";
  public boolean isAgent = false;


  private String m_userName;
  private String m_password;
  private String m_url = "jdbc:oracle:thin:@gwynne.cs.ualberta.ca:1521:CRS";

  public static void main(String args[]) {
    Application app = new Application();
    Scanner in = new Scanner(System.in);

    try
    {
      Class drvClass = Class.forName("oracle.jdbc.driver.OracleDriver");
      DriverManager.registerDriver((Driver)
      drvClass.newInstance());
    } catch(Exception e)
    {
      System.err.print("ClassNotFoundException: ");
      System.err.println(e.getMessage());
    }

    app.m_userName = args[0];
    app.m_password = args[1];

    System.out.println("Pick an option:");
    System.out.println("1 - Registered User");
    System.out.println("2 - Not a Registered User");
    System.out.println("q - Exit");
    String result = in.nextLine();

    if (result.equals("1")){

      while(app.Login(app) == false){}
        System.out.println("Valid Credentials!");
        app.Menu(app);
      }
      else if(result.equals("2")){
        app.createUser(app);
      }
      else if(result.equals("q")){

      }

    }

    //Display main menu
    public void Menu(Application app)
    {
      Scanner in = new Scanner(System.in);
      System.out.println("\nWhat would you like to do? Choose an option.");
      System.out.println("1 - Search for flight");
      System.out.println("2 - View existing bookings");
      if (app.isAgent)
      {
        System.out.println("3 - Record Departure");
        System.out.println("4 - Record Arrival");
      }
      System.out.println("0 - Logout");

      int choice = in.nextInt();
      if (choice == 1)
      {
        //search for flight
        app.searchFlights(app);
      }
      else if (choice == 2)
      {
        System.out.println();
        //View bookings
        app.viewBookings(app);
      }
      else if (choice == 3)
      {
        //Update departure
        app.updateDeparture(app);
      }
      else if (choice == 4)
      {
        //Update arrival
        app.updateArrival(app);
      }
      else if (choice == 0)
      {
        //log out
        app.Logout(app);
      }
    }

    public void searchFlights(Application app) {
      Scanner in = new Scanner(System.in);
      String two_connections = "";
      boolean round_trip = false;
      String sortOptions = "(order by price asc) as rn ";
      String dep_date, ret_date = "";

      System.out.print("Do you want to book a round trip? (y/n): ");
      if (in.next().toLowerCase().equals("y")){
        round_trip = true;
      }
      System.out.print("Would you like to sort by number of stops first before the price? (y/n): ");
      if(in.next().toLowerCase().equals("y")){
        sortOptions = "(order by stops asc, price asc) as rn ";
      }

      System.out.print("\nEnter source: ");
      String src = in.next();
      //check for acode, city, or name
      src = app.findAcode(app, src);

      System.out.print("\nEnter destination: ");
      String dst = in.next();
      //check for acode, city, or name
      dst = app.findAcode(app, dst);

      System.out.print("\nEnter departure date (DD-MM-YYYY): ");
      dep_date = in.next();
      String[] dep_dateparts = dep_date.split("-");
      String[] ret_dateparts = dep_date.split("-");
      while (Integer.parseInt(dep_dateparts[0]) < 0 || Integer.parseInt(dep_dateparts[0]) > 31 || Integer.parseInt(dep_dateparts[1]) < 0 || Integer.parseInt(dep_dateparts[1]) > 12
      || Integer.parseInt(dep_dateparts[2]) < 2000 || Integer.parseInt(dep_dateparts[2]) > 2200 || dep_dateparts.length != 3){
        System.out.print("Please print a valid departure date (DD-MM-YYYY): ");
        dep_date = in.next();
        dep_dateparts = dep_date.split("-");
      }

      if (round_trip == true){
        System.out.print("\nEnter a return date (DD-MM-YYYY): ");
        ret_date = in.next();
        ret_dateparts = ret_date.split("-");
        while (Integer.parseInt(ret_dateparts[0]) < 0 || Integer.parseInt(ret_dateparts[0]) > 31 || Integer.parseInt(ret_dateparts[1]) < 0 || Integer.parseInt(ret_dateparts[1]) > 12
        || Integer.parseInt(ret_dateparts[2]) < 2000 || Integer.parseInt(ret_dateparts[2]) > 2200 || ret_dateparts.length != 3){
          System.out.print("Please print a valid return date (DD-MM-YYYY): ");
          ret_date = in.next();
          ret_dateparts = ret_date.split("-");
        }
      }
        app.initViews(app);

        System.out.print("Do you want to include flights that have 2 connections? (y/n): ");
        if (in.next().toLowerCase().equals("y")){
          //What to add to the query to include two connections (Union)
          two_connections =
                      "union " +
                      "SELECT flightno as fno, '' fno2, '' fno3, to_char(dep_date, 'DD-MM-YYYY') as dep_date, src,dst,to_char(dep_time, 'HH24:MI') as dep, " +
                      "to_char(arr_time, 'HH24:MI') as arr,fare,price, 2 stops " +
                      "FROM two_connections " +
                      "WHERE src = '" + src + "' and dst = '" + dst + "' " +
                      "AND to_char(dep_date, 'DD-MM-YYYY') = '" + dep_date + "' ";
        }

        Connection m_con;
        String flights, ret_flights;
        //Display flights with specifications given by user
        flights = "SELECT rn, fno, fno2, fno3, dep_date, src, dst, dep, arr, price, stops " +
                  "FROM ( " +
                    "SELECT fno, fno2, fno3, dep_date, src, dst, dep, arr, price, stops, row_number() over "+ sortOptions +
                    "FROM ( " +
                      "SELECT flightno as fno, '' fno2, '' fno3, to_char(dep_date, 'DD-MM-YYYY') as dep_date, src,dst,to_char(dep_time, 'HH24:MI') as dep, " +
                      "to_char(arr_time, 'HH24:MI') as arr,price, 0 stops " +
                      "FROM available_flights " +
                      "WHERE src = '" + src + "' and dst = '" + dst + "' " +
                      "AND to_char(dep_date, 'DD-MM-YYYY') = '" + dep_date + "' " +
		                  "UNION " +
		                  "SELECT flightno1 as fno, flightno2 as fno2, '' fno3, to_char(dep_date, 'DD-MM-YYYY') as dep_date, src,dst,to_char(dep_time, 'HH24:MI') as dep, " +
                      "to_char(arr_time, 'HH24:MI') as arr,price, 1 stops " +
                      "FROM one_connection " +
                      "WHERE src = '" + src + "' and dst = '" + dst + "' " +
                      "AND to_char(dep_date, 'DD-MM-YYYY') = '" + dep_date + "' " +
                      two_connections +
                  ")) WHERE rn <=5";
                  //"AND extract(day from dep_date) = '" + dep_dateparts[0] + "'" +
                  //"AND extract(month from dep_date) = '" + dep_dateparts[1] + "'" +
                  //"AND extract(year from dep_date) = '" + dep_dateparts[2] + "'";

        //Display return flights (not really the right way to do this, needs fixing)
        ret_flights = "SELECT flightno as fno, to_char(dep_date, 'DD-MM-YYYY') as dep_date, src,dst,to_char(dep_time, 'HH24:MI') as dep, " +
                  "to_char(arr_time, 'HH24:MI') as arr,fare,seats,price " +
                  "FROM available_flights " +
                  "WHERE src = '" + dst + "' and dst = '" + src + "'" +
                  "AND to_char(dep_date, 'DD-MM-YYYY') = '" + ret_date + "'";
                  //"AND extract(day from dep_date) = '" + ret_dateparts[0] + "'" +
                  //"AND extract(month from dep_date) = '" + ret_dateparts[1] + "'" +
                  //"AND extract(year from dep_date) = '" + ret_dateparts[2] + "'";

        Statement stmt;
        Statement stmt2;

        try
        {
          m_con = DriverManager.getConnection(app.m_url, app.m_userName, app.m_password);

          stmt = m_con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
          stmt2 = m_con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
          ResultSet rst = stmt.executeQuery(flights);

          if(!rst.next())
          {
            System.out.println("No flights found.");
          }
          else
          {
            System.out.println("Available departing flights: ");
            app.displayResultSet(rst);
            //Code asking to select a number based on ranking
          }
          if(round_trip)
          {
            ResultSet rst2 = stmt2.executeQuery(ret_flights);
            if (!rst2.next()){
              System.out.println("No return flights available.");
            }
            else{
              System.out.println("Available returning flights: ");
              displayResultSet(rst2);
              //Code asking to select a number based on ranking
            }
          }

          rst.close();
          stmt.close();
          m_con.close();
        } catch(SQLException ex) {
          System.err.println("SQLException: " +
          ex.getMessage());
        }

        app.Menu(app);



    }

    public void initViews(Application app)
    {

      Connection m_con;
      String dropView1, dropView2, dropView3, singleFlight, twoFlights, threeFlights;
      dropView1 = "DROP VIEW available_flights";
      dropView2 = "DROP VIEW one_connection";
      dropView3 = "DROP VIEW two_connections";
      singleFlight = "create view available_flights(flightno,dep_date, src,dst,dep_time,arr_time,fare,seats,price) as " +
                      "select f.flightno, sf.dep_date, f.src, f.dst, f.dep_time+(trunc(sf.dep_date)-trunc(f.dep_time)), " +
                      "f.dep_time+(trunc(sf.dep_date)-trunc(f.dep_time))+(f.est_dur/60+a2.tzone-a1.tzone)/24, " +
                      "fa.fare, fa.limit-count(tno), fa.price " +
                      "from flights f, flight_fares fa, sch_flights sf, bookings b, airports a1, airports a2 " +
                      "where f.flightno=sf.flightno and f.flightno=fa.flightno and f.src=a1.acode and " +
                      "f.dst=a2.acode and fa.flightno=b.flightno(+) and fa.fare=b.fare(+) and " +
                      "sf.dep_date=b.dep_date(+) " +
                      "group by f.flightno, sf.dep_date, f.src, f.dst, f.dep_time, f.est_dur,a2.tzone, " +
                      "a1.tzone, fa.fare, fa.limit, fa.price " +
                      "having fa.limit-count(tno) > 0";
      twoFlights = "create view one_connection (src,dst,dep_date,flightno1,flightno2, layover,price, dep_time, arr_time) as " +
			                "select a1.src, a2.dst, a1.dep_date, a1.flightno, a2.flightno, a2.dep_time-a1.arr_time, " +
			                "min(a1.price+a2.price), a1.dep_time, a2.arr_time " +
			                "from available_flights a1, available_flights a2 " +
			                "where a1.dst=a2.src and a1.arr_time +1.5/24 <=a2.dep_time and a1.arr_time +5/24 >=a2.dep_time " +
			                "group by a1.src, a2.dst, a1.dep_date, a1.flightno, a2.flightno, a2.dep_time, a1.arr_time, a1.dep_time, a2.arr_time ";
      threeFlights = "create view two_connections (src,dst,dep_date,flightno1,flightno2, flightno3, layover,price, dep_time, arr_time) as " +
			                  "select a1.src, a3.dst, a1.dep_date, a1.flightno, a2.flightno, a3.flightno, (a2.dep_time-a1.arr_time+(a3.dep_time-a2.arr_time)), " +
			                  "min(a1.price+a2.price+a2.price), a1.dep_time, a3.arr_time " +
			                  "from available_flights a1, available_flights a2, available_flights a3 " +
			                  "where a1.dst=a2.src and a2.dst=a3.src and a1.arr_time +1.5/24 <=a2.dep_time and a1.arr_time +5/24 >=a2.dep_time and a2.arr_time +1.5/24 <=a3.dep_time and a2.arr_time +5/24 >=a3.dep_time " +
			                  "group by a1.src, a3.dst, a1.dep_date, a1.flightno, a2.flightno, a3.flightno, a2.dep_time, a1.arr_time, a3.dep_time, a2.arr_time, a1.dep_time, a3.arr_time ";
      Statement stmt;



      try
      {
        m_con = DriverManager.getConnection(app.m_url, app.m_userName, app.m_password);

        stmt = m_con.createStatement();
        stmt.executeQuery(dropView1);
	      stmt.executeQuery(dropView2);
	      stmt.executeQuery(dropView3);

        stmt.close();
        m_con.close();
      } catch(SQLException ex) {

      }
      try
      {
        m_con = DriverManager.getConnection(app.m_url, app.m_userName, app.m_password);

        stmt = m_con.createStatement();
        stmt.executeQuery(singleFlight);
	      stmt.executeQuery(twoFlights);
	      stmt.executeQuery(threeFlights);

        stmt.close();
        m_con.close();
      } catch(SQLException ex) {
        System.err.println("SQLException: " +
        ex.getMessage());
      }
      return;
    }


    public void viewBookings(Application app)
    {
      Scanner in = new Scanner(System.in);
      String findBookings;
      //Find booking information related to client email
      findBookings = "select b.tno, to_char(dep_date, 'DD-Mon-YYYY') as dep_date, paid_price, name " +
      "from bookings b, tickets t " +
      "where b.tno = t.tno " +
      "and t.email = '" + app.client_email +"'";
      Statement stmt;
      Statement stmt2;
      Connection con;
      try
      {
        con = DriverManager.getConnection(app.m_url, app.m_userName, app.m_password);
        stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
        stmt2 = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);

        ResultSet rs = stmt.executeQuery(findBookings);


        displayResultSet(rs);


        System.out.println("Choose an option: ");
        System.out.println("1 - View more details about a particular booking");
        System.out.println("2 - Cancel a booking");
        System.out.println("0 - Return to main menu");
        System.out.print("Choice: ");
        int input = in.nextInt();
        if(input == 2)
        {
          System.out.print("Enter ticket number of booking you would like to cancel: ");
          input = in.nextInt();

          //Check if ticket number is valid
          String cancel = "select b.tno from bookings b, tickets t " +
          "where b.tno = t.tno " +
          "and t.email = '" + app.client_email + "' " +
          "and b.tno = '" + input + "'";
          ResultSet rs3 = stmt.executeQuery(cancel);
          //Check for valid ticket number
          if (!rs3.next())
          {
            System.out.println("Invalid ticket number. Returning to bookings menu...");
            try
            {
              Thread.sleep(1500);
            } catch(InterruptedException ex)
            {
              Thread.currentThread().interrupt();
            }
            System.out.println();
            app.viewBookings(app);
          }
          else
          {
            //Double check cancellation
            System.out.print("Are you sure you want to cancel the booking assciated with ticket number " + input + "? (y/n)");
            char input2 = in.next().charAt(0);
            //Cancel booking
            if (input2 == 'y' || input2 == 'Y')
            {
              String cancelBooking = "delete from bookings where tno = '" + input + "'";
              String cancelBooking2 = "delete from tickets where tno = '" + input + "'";
              stmt.executeUpdate(cancelBooking);
              stmt.executeUpdate(cancelBooking2);
              System.out.println("Booking has successfully been cancelled. Returning to bookings menu...");
              try
              {
                Thread.sleep(1500);
              } catch(InterruptedException ex)
              {
                Thread.currentThread().interrupt();
              }
              app.viewBookings(app);
            }
          }

        }
        else if (input == 1)
        {
          System.out.print("Enter ticket number of booking you would like to see details about: ");
          input = in.nextInt();

          //Check if ticket number is valid
          String check = "select b.tno from bookings b, tickets t " +
          "where b.tno = t.tno " +
          "and t.email = '" + app.client_email + "' " +
          "and b.tno = '" + input + "'";
          ResultSet rs4 = stmt.executeQuery(check);
          if (!rs4.next())
          {
            System.out.println("Invalid ticket number. Returning to bookings menu...");
            try
            {
              Thread.sleep(1500);
            } catch(InterruptedException ex)
            {
              Thread.currentThread().interrupt();
            }
            System.out.println();
            app.viewBookings(app);
          }

          //Display extra info
          String moreInfo = "select distinct b.fare, bag_allow, b.flightno, src, dst, est_dur " +
          "from bookings b, tickets t, flight_fares ff, flights f " +
          "where b.tno = t.tno " +
          "and f.flightno = b.flightno " +
          "and b.fare = ff.fare " +
          "and t.email = '" + app.client_email +"'" +
          "and b.tno = '" + input + "'";

          ResultSet rs2 = stmt.executeQuery(moreInfo);
          System.out.println();

          ResultSet rs6 = stmt2.executeQuery(findBookings);
          displayResultSet(rs6);
          displayResultSet(rs2);
          System.out.println("Enter 1 to return to bookings menu, or 2 to return to main menu.");
          int input3 = in.nextInt();
          if (input3 == 1)
          {
            app.viewBookings(app);
          }
          else
          {
            app.Menu(app);
          }
        }
        else if (input == 0)
        {
          app.Menu(app);
        }
        stmt.close();
        stmt2.close();
        con.close();
      } catch(SQLException ex){
        System.out.println(ex);
      }
    }


    public String findAcode(Application app, String input) {
      Scanner in = new Scanner(System.in);
      Boolean found = false;
      Connection m_con;
      String findAcode;
      findAcode = "SELECT acode, city, name FROM airports";
      Statement stmt;


      try
      {
        m_con = DriverManager.getConnection(app.m_url, app.m_userName, app.m_password);

        stmt = m_con.createStatement();
        ResultSet rst = stmt.executeQuery(findAcode);
        while(rst.next()){
          if (input.equalsIgnoreCase(rst.getString(1).trim())){
            found = true;
            System.out.print("Selected airport: ");
            System.out.println(rst.getString(1).trim() + " - " +  rst.getString(2).trim() + ", " +  rst.getString(3).trim());
            return rst.getString(1);
          }
          else if (rst.getString(2).toLowerCase().contains(input.toLowerCase()) || rst.getString(3).toLowerCase().contains(input.toLowerCase())){
            System.out.println(rst.getString(1).trim() + " - " +  rst.getString(2).trim() + ", " +  rst.getString(3).trim());
          }
        }
        rst.close();
        stmt.close();
        m_con.close();
      } catch(SQLException ex) {
        System.err.println("SQLException: " +
        ex.getMessage());

      }

      System.out.print("Enter the acode of the airport you want: ");
      String acode = in.next();
      acode = app.findAcode(app, acode);

      return acode;
    }


    public void updateDeparture(Application app){

      Scanner in = new Scanner(System.in);

      System.out.print("What is the flight number of the departure: ");
      String flightnum = in.next().toLowerCase();
      System.out.print("What is the date of this flight (DD-Mon-YYYY): ");
      String date = in.next();

      System.out.print("What was the departure time (HH24-MI): ");
      String departure = in.next();



      Connection m_con;
      String updateDeparture;

      updateDeparture = "UPDATE sch_flights SET act_dep_time = to_date('"+ departure +"', 'HH24-MI') WHERE flightno = '" + flightnum + "' and dep_date = to_date('"+ date +"', 'DD-Mon-YY') ";

      Statement stmt;

      try
      {
        m_con = DriverManager.getConnection(app.m_url, app.m_userName, app.m_password);

        stmt = m_con.createStatement();
        stmt.executeUpdate(updateDeparture);



        stmt.close();
        m_con.close();

      } catch(SQLException ex) {
        if (ex.getErrorCode() == 1840 || ex.getErrorCode() == 1858){
          System.out.println("Please input a date/time with the proper format...");
          app.updateDeparture(app);
        }

      }
      app.Menu(app);
    }

    public void updateArrival(Application app){

      Scanner in = new Scanner(System.in);

      System.out.print("What is the flight number of the departure: ");
      String flightnum = in.next().toLowerCase();
      System.out.print("What is the date of this flight (DD-Mon-YYYY): ");
      String date = in.next();


      System.out.print("What was the arrival time (HH24-MI): ");
      String arrival = in.next();



      Connection m_con;
      String updateArrival;

      updateArrival = "UPDATE sch_flights SET act_arr_time = to_date('"+ arrival +"', 'HH24-MI') WHERE flightno = '" + flightnum + "' and dep_date = to_date('"+ date +"', 'DD-Mon-YY') ";

      Statement stmt;

      try
      {
        m_con = DriverManager.getConnection(app.m_url, app.m_userName, app.m_password);

        stmt = m_con.createStatement();
        stmt.executeUpdate(updateArrival);



        stmt.close();
        m_con.close();

      } catch(SQLException ex) {
        if (ex.getErrorCode() == 1840 || ex.getErrorCode() == 1858){
          System.out.println("Please input a date/time with the proper format...");
          app.updateArrival(app);
        }

      }
      app.Menu(app);
    }

    //function to create a new user
    public void createUser(Application app)
    {
      //Get user information
      Scanner in = new Scanner(System.in);
      System.out.print("\nPlease enter an email address: ");
      app.client_email = in.next();
      System.out.print("Enter a password: ");
      app.client_password = in.next();

      //Check for password length
      if (app.client_password.length() > 4) {
        System.out.println("Password is too long; maximum 4 char");
        app.createUser(app);
      }

      Connection m_con;
      String updateTable;
      //Add user email and password to table Users
      updateTable = "insert into users values" + "('" + app.client_email + "', '" + app.client_password + "', SYSDATE)";

      Statement stmt;

      try
      {
        m_con = DriverManager.getConnection(app.m_url, app.m_userName, app.m_password);

        stmt = m_con.createStatement();
        stmt.executeUpdate(updateTable);
        stmt.close();
        m_con.close();
        System.out.println("Successfully created account");
      } catch(SQLException ex) {

        if (ex.getErrorCode() == 1){
          System.out.println("An account with that email already exists!");
        }
        app.createUser(app);
      }
      //Return to menu
      app.Menu(app);
    }

    public boolean Login(Application app){
      Scanner in = new Scanner(System.in);
      boolean valid = false;

      System.out.print("\nPlease enter your email: ");
      app.client_email = in.next();
      System.out.print("Please enter your password: ");
      app.client_password = in.next();

      Connection m_con;
      String findUsers, findAgents;
      findUsers = "SELECT email, pass FROM users";
      findAgents = "SELECT email from airline_agents";
      Statement stmt;

      try
      {
        m_con = DriverManager.getConnection(app.m_url, app.m_userName, app.m_password);

        stmt = m_con.createStatement();
        ResultSet rst = stmt.executeQuery(findUsers);
        while(rst.next()){
          if (app.client_email.equals(rst.getString(1).trim()) && app.client_password.equals(rst.getString(2).trim())){
            valid = true;

            break;
          }
        }
        rst.close();
        stmt.close();
        m_con.close();
      } catch(SQLException ex) {
        System.err.println("SQLException: " +
        ex.getMessage());
      }

      try
      {
        m_con = DriverManager.getConnection(app.m_url, app.m_userName, app.m_password);

        stmt = m_con.createStatement();
        ResultSet rst = stmt.executeQuery(findAgents);
        while(rst.next()){
          if (app.client_email.equals(rst.getString(1).trim())){
            app.isAgent = true;


            break;
          }
        }
        rst.close();
        stmt.close();
        m_con.close();
      } catch(SQLException ex) {
        System.err.println("SQLException: " +
        ex.getMessage());
      }
      if (valid == false){
        System.out.println("That is not a valid username/password combination!");
      }
      return valid;
    }



    public void Logout(Application app)
    {

      Scanner in = new Scanner(System.in);

      Connection m_con;
      String updateLogin;
      //Add user email and password to table Users. Not sure what to initialize the date to
      updateLogin = "UPDATE users SET last_login = SYSDATE WHERE email = '" + app.client_email +"'";

      Statement stmt;

      try
      {
        m_con = DriverManager.getConnection(app.m_url, app.m_userName, app.m_password);

        stmt = m_con.createStatement();
        stmt.executeUpdate(updateLogin);



        stmt.close();
        m_con.close();

      } catch(SQLException ex) {
        System.err.println("SQLException: " +
        ex.getMessage());

      }


    }


    //Function for displaying a result set, with column names
    public void displayResultSet(ResultSet rs)
 {
   System.out.println("-----------------------------------------------------------------------------");
   String value = null;
   Object o = null;

   try
   {
    ResultSetMetaData rsM = rs.getMetaData();

     int columnCount = rsM.getColumnCount();

    //Print column names
    for (int column = 1; column <= columnCount; column++)
     {
       value = rsM.getColumnLabel(column);
       System.out.print(value + "\t");
     }
     System.out.println();
     System.out.println("-----------------------------------------------------------------------------");

     while(rs.next())
     {
       //Print row values
       for (int i = 1; i <= columnCount; i++)
       {
         o = rs.getObject(i);
         if (o != null)
         {
           value = o.toString();
         }
         else
         {
           value = "null";
         }
         System.out.print(value + "\t");
         //Extra tabbing for nicer formatting of display
         if (rsM.getColumnLabel(i).equals("BAG_ALLOW"))
         {
           System.out.print("\t");
         }
         if (rsM.getColumnLabel(i).equals("FLIGHTNO"))
         {
           System.out.print("\t");
         }
         if (rsM.getColumnLabel(i).equals("PAID_PRICE"))
         {
           System.out.print("\t");
         }
       }
     System.out.println();
     System.out.println("-----------------------------------------------------------------------------");
     }
   } catch(Exception io)
   {
     System.out.println(io.getMessage());
   }
 }


  }
