/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */

package net.wage.calculation;

/**
 *
 * @author jrbusadre
 */

// This is to import necessary libraries for working with excel files and user input
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class NetWageCalculation {
 // This is to declare the employee's basic hourly wage
    private static final double HOURLY_RATE = 100.0;

    public static void main(String[] args) {
        // This is to start the payroll system and controls the main workflow
        System.out.println("===== MotorPH Payroll System =====\n");

        String filePath = "src/Copy of MotorPH Employee Data.xlsx"; // File location of the employee data
        Scanner scanner = new Scanner(System.in); // Scanner to accept user input

        try {
            // This is to open the Excel file and loads the workbook
            FileInputStream fis = new FileInputStream(new File(filePath));
            Workbook workbook = new XSSFWorkbook(fis);

            // This is to access the specific sheets for employee info and attendance logs
            Sheet employeeSheet = workbook.getSheet("Employee Details");
            Sheet attendanceSheet = workbook.getSheet("Attendance Record");

            // This is to validate the existence of required sheets
            if (employeeSheet == null || attendanceSheet == null) {
                System.out.println("Required sheets not found in the Excel file.");
                return;
            }

            // This is to loop to repeatedly prompt user for employee number until they choose to stop
            boolean continueSearch = true;
            while (continueSearch) {
                String inputEmpNo = promptEmployeeNumber(scanner); // To get employee number from user
                boolean found = searchAndDisplayEmployee(employeeSheet, attendanceSheet, inputEmpNo); // To retrieve and process data
                if (!found) {
                    System.out.println("Employee number not found. Please try again.\n");
                    continue;
                }
                continueSearch = askToSearchAgain(scanner); // To check if user wants to process another employee
            }

            System.out.println("\nThank you for using MotorPH Payroll System!");

        } catch (IOException e) {
            // This is to catche and reports errors related to file access
            System.out.println("Error reading Excel file: " + e.getMessage());
        }
    }

    // This is to collect the employee number input from the user
    private static String promptEmployeeNumber(Scanner scanner) {
        System.out.print("Enter Employee Number: ");
        return scanner.nextLine().trim();
    }

    // This is to retrieve employee record and calculates their weekly salary and deductions
    private static boolean searchAndDisplayEmployee(Sheet employeeSheet, Sheet attendanceSheet, String empNoInput) {
        for (Row row : employeeSheet) {
            if (row.getRowNum() == 0) continue; // To skips header row
            String empNo = getCellValueAsString(row.getCell(0));
            if (empNo.equals(empNoInput)) {
                String lastName = getCellValueAsString(row.getCell(1));
                String firstName = getCellValueAsString(row.getCell(2));
                String birthday = formatDateCell(row.getCell(3));

                printEmployeeDetails(empNo, firstName, lastName, birthday);

                double totalHours = calculateOneWeekHoursWorked(attendanceSheet, empNo);
                System.out.printf("Total Hours Worked (Last 7 Days): %.2f hours\n", totalHours);

                double grossSalary = totalHours * HOURLY_RATE;
                System.out.printf("Gross Salary: PHP %.2f\n", grossSalary);

                // Government-mandated deductions
                double sss = estimateSSS(grossSalary);
                double pagibig = estimatePagibig(grossSalary);
                double philhealth = estimatePhilhealth(grossSalary);
                double incomeTax = estimateWithholdingTax(grossSalary);

                // Final salary after subtracting deductions
                double totalDeductions = sss + pagibig + philhealth + incomeTax;
                double netSalary = grossSalary - totalDeductions;

                // This is to display deduction breakdown and net pay
                System.out.printf("SSS Deduction: PHP %.2f\n", sss);
                System.out.printf("Pag-IBIG Deduction: PHP %.2f\n", pagibig);
                System.out.printf("PhilHealth Deduction: PHP %.2f\n", philhealth);
                System.out.printf("Withholding Tax: PHP %.2f\n", incomeTax);
                System.out.printf("Net Salary: PHP %.2f\n", netSalary);

                System.out.println("========================================");
                return true;
            }
        }
        return false;
    }

    // This is to compute the total working hours from the latest 7 attendance entries
    private static double calculateOneWeekHoursWorked(Sheet attendanceSheet, String empNoInput) {
        Map<Date, Row> dateRowMap = new TreeMap<>(Collections.reverseOrder()); // To stores rows by date in descending order
        for (Row row : attendanceSheet) {
            if (row.getRowNum() == 0) continue;
            String empNo = getCellValueAsString(row.getCell(0));
            if (empNo.equals(empNoInput)) {
                Cell dateCell = row.getCell(3);
                if (dateCell != null && DateUtil.isCellDateFormatted(dateCell)) {
                    dateRowMap.put(dateCell.getDateCellValue(), row);
                }
            }
        }

        int count = 0;
        double totalHours = 0.0;
        for (Date date : dateRowMap.keySet()) {
            if (count >= 7) break; // Only process the last 7 days
            Row row = dateRowMap.get(date);
            Cell loginCell = row.getCell(4);
            Cell logoutCell = row.getCell(5);

            // This is to validate cells before calculating time duration
            if (loginCell != null && logoutCell != null
                    && loginCell.getCellType() == CellType.NUMERIC
                    && logoutCell.getCellType() == CellType.NUMERIC
                    && DateUtil.isCellDateFormatted(loginCell)
                    && DateUtil.isCellDateFormatted(logoutCell)) {

                Date login = loginCell.getDateCellValue();
                Date logout = logoutCell.getDateCellValue();
                long durationMillis = logout.getTime() - login.getTime();
                totalHours += durationMillis / (1000.0 * 60 * 60); // To convert milliseconds to hours
                count++;
            }
        }
        return totalHours;
    }

    // This is to return a placeholder value representing the SSS deduction
    private static double estimateSSS(double grossSalary) {
        return 225.0;
    }

    // This is to compute Pag-IBIG deduction, with a max contribution of PHP 100
    private static double estimatePagibig(double grossSalary) {
        return Math.min(grossSalary * 0.02, 100);
    }

    // This is to determine employee’s share of the PhilHealth contribution
    private static double estimatePhilhealth(double grossSalary) {
        double monthlyRate = grossSalary * 0.03;
        return monthlyRate / 2;
    }

    // This is to calculate income tax based on BIR tax bracket structure
    private static double estimateWithholdingTax(double grossSalary) {
        if (grossSalary <= 20832) return 0;
        else if (grossSalary <= 33332) return (grossSalary - 20833) * 0.2;
        else if (grossSalary <= 66667) return 2500 + (grossSalary - 33333) * 0.25;
        else if (grossSalary <= 166667) return 10833 + (grossSalary - 66667) * 0.3;
        else return 40833.33 + (grossSalary - 166667) * 0.32;
    }

    // This is to displays key personal details of an employee
    private static void printEmployeeDetails(String empNo, String firstName, String lastName, String birthday) {
        System.out.println("\n========= Employee Information =========");
        System.out.println("Employee No: " + empNo);
        System.out.println("Name: " + firstName + " " + lastName);
        System.out.println("Birthday: " + birthday);
    }

    // This is to prompts the user whether to continue processing more employees
    private static boolean askToSearchAgain(Scanner scanner) {
        System.out.print("Do you want to search again? (y/n): ");
        String answer = scanner.nextLine().trim().toLowerCase();
        return answer.equals("y");
    }

    // This is to convert an Excel cell to a readable string, handling text, numbers, and dates
    private static String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
                    yield sdf.format(cell.getDateCellValue());
                } else {
                    yield String.valueOf((int) cell.getNumericCellValue());
                }
            }
            default -> "";
        };
    }

    // This is to extract and formats date values from Excel cells into standard MM/dd/yyyy strings
    private static String formatDateCell(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
            return sdf.format(cell.getDateCellValue());
        }
        return cell.getStringCellValue();
    }
}

