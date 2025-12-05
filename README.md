# Senior_Project_Fall_2025-Group_33
Group 33 Senior Project at UTD, Fall 2025

## Setting up MySQL Connection
- On MySQL Workbench the user will create a  Standard TCP/IP connection with the Username "root"
- Anything for the Password but as it is not an important DB, no password is recommended
- Select a usable port number
    - Test the connection before adding details to the JavaFX files
- Save the Username & Password
- Save & Exit the connection edit tab and right click the connection to then copy the JDBC URL

## Setting up JDBC Connection & Manager Class
- The section of code listed is in both classes    
```
private static final String URL = "jdbc:mysql://localhost:3306/new_schema"; // Put your database name
private static final String USER = "root"; // Put your username
private static final String PASSWORD = "Password"; // Put your database password
```
- Paste the JDBC URL inplace of the URL shown
    - Make sure that only the hostname:Port Number/new_schema are in the URL
- Finally put your Username & Password in place of the instances already in the classes
