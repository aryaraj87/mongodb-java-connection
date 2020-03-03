package com;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoException;

public class DBConnection {

    static DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH);
    static String FILE_PATH = "D:\\Report\\Report_";
    static String FILE_EXTENSION = ".xls";
	static DateFormat df = new SimpleDateFormat("yyyyMMddhhmmss"); // add S if you need milliseconds
	private static final String filename = FILE_PATH + df.format(new Date()) + "." + FILE_EXTENSION;
	static Map<String,List<String>> finalReport = new HashMap<String,List<String>>();
	static int rwCnt = 0;
	static Row row =  null;
	static Sheet lastSheet = null;
	static OutputStream fos1 = null;
	static Workbook workbk = null;
	
	
	public static void main(String[] args) {
		 
		 try {
		 
		  Mongo mongo = new Mongo("batch-corona.austin.hpicorp.net", 37017);
		  DB db = mongo.getDB("coronaqids");
		  boolean auth = db.authenticate("coronatest", "test".toCharArray());
		  if(auth){
			  DBCollection collection = db.getCollection("quotes");
				/*
				 * qidsQuery(collection); optimusQuery(collection);
				 */
			  mdmQuery(collection);
			  //edmsQuery(collection);
			  generateFinalReport(finalReport);
		  }else{
		   System.out.println("No DB Connection");
		  }
		  System.out.println("Done");
		  } catch (UnknownHostException e) {
		   e.printStackTrace();
		  } catch (MongoException e) {
		   e.printStackTrace();
		  } catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 
		 }
	
static void qidsQuery(DBCollection collection) throws ParseException {
	System.out.println("**** QIDS - Begins ****");
	BasicDBObject whereQuery = new BasicDBObject();
	whereQuery.put("eventId", "INT_QIDS"); 
	whereQuery.put("created_at", new BasicDBObject("$gte",format.parse("2020-02-24T15:58:00Z")));
	generateReport(collection,whereQuery,"QIDS");
	System.out.println("**** QIDS - Ends ****");
}
static void optimusQuery(DBCollection collection) throws ParseException {
	System.out.println("**** OPTIMUS - Begins ****");
	BasicDBObject whereQuery = new BasicDBObject();
	whereQuery.put("eventId", "INT_PROS_INTERNAL"); 
	whereQuery.put("created_at", new BasicDBObject("$gte",format.parse("2020-02-24T05:30:00Z")));
	generateReport(collection,whereQuery,"OPTIMUS");
	System.out.println("**** OPTIMUS - Ends ****");
}
static void mdmQuery(DBCollection collection) throws ParseException {
	System.out.println("**** MDM - Begins ****");
	BasicDBObject whereQuery = new BasicDBObject();
	BasicDBObject timeStamp = new BasicDBObject();
	timeStamp.put("$gte",format.parse("2020-02-19T15:19:00Z"));
	timeStamp.put("$lt",format.parse("2020-02-24T00:01:59Z"));
	whereQuery.put("eventId", "INT_MDM_OA"); 
	whereQuery.put("created_at", timeStamp);
	generateReport(collection,whereQuery,"MDM");
	System.out.println("**** MDM - Ends ****");
}
static void edmsQuery(DBCollection collection) throws ParseException {
	System.out.println("**** EDMS - Begins ****");
	BasicDBObject whereQuery = new BasicDBObject();
	whereQuery.put("eventId", "INT_ECLIPSE_EDMS"); 
	whereQuery.put("created_at", new BasicDBObject("$gte",format.parse("2020-02-23T00:01:59Z")));
	generateReport(collection,whereQuery,"EDMS");
	System.out.println("**** EDMS - Ends ****");
}
static void generateReport(DBCollection collection,BasicDBObject whereQuery,String applicationName) {
	File file = null;
	OutputStream fos = null;
	Workbook workbook = null;
	try {
		file = new File(filename);
		Sheet sheet = null;
		if (file.exists()) {
			workbook = (HSSFWorkbook) WorkbookFactory.create(new FileInputStream(filename));
		} else {
			workbook = new HSSFWorkbook();
		}
		sheet = workbook.createSheet(applicationName);
		List<String> fieldNames = new ArrayList<String>();
		fieldNames.add("EventId");
		fieldNames.add("EventDate");
		fieldNames.add("EventDetails");
		fieldNames.add("TranscationId");
		int rowCount = 0;
		int columnCount = 0;
		Row row = sheet.createRow(rowCount++);
		for (String fieldName : fieldNames) {
			Cell cell = row.createCell(columnCount++);
			cell.setCellValue(fieldName);
		}
		BasicDBObject fields = new BasicDBObject();
	    fields.put("eventId",1);
	    fields.put("eventDate",1);
	    fields.put("eventDetails",1);
	    fields.put("transactionId",1);
		DBCursor cursor = collection.find(whereQuery,fields).sort(new BasicDBObject("created_at",-1));
		TreeMap<Double, String> sortedeventDetails = new TreeMap<Double, String>();
	    while (cursor.hasNext()) {
	    	DBObject tobj = cursor.next();
			row = sheet.createRow(rowCount++);
			Cell cell0 = row.createCell(0);
			cell0.setCellValue((String)tobj.get("eventId"));
			Cell cell1 = row.createCell(1);
			cell1.setCellValue((String)tobj.get("eventDate"));
			Cell cell2 = row.createCell(2);
			cell2.setCellValue((String)tobj.get("eventDetails"));
			Cell cell3 = row.createCell(3);
			cell3.setCellValue((String)tobj.get("transactionId"));
			try {
	            Double num = Double.parseDouble((String)tobj.get("eventDetails"));
	            sortedeventDetails.put(num, (String)tobj.get("eventDate"));
	        } catch (NumberFormatException e) {
	            
	        }
			
		}
	    if(!sortedeventDetails.isEmpty()) {
		    List<String> eventDetails = new ArrayList<String>();
		    eventDetails.add(sortedeventDetails.firstKey().toString());
		    eventDetails.add(sortedeventDetails.lastKey().toString());
		    eventDetails.add(sortedeventDetails.lastEntry().getValue());
		    eventDetails.add(averageOfEvent(sortedeventDetails));
		    finalReport.put(applicationName,eventDetails);
	    }
		fos = new FileOutputStream(file);
		workbook.write(fos);
		fos.flush();
	} catch (Exception e) {
		e.printStackTrace();
	} finally {
		try {
			if (fos != null) {
				fos.close();
			}
		} catch (IOException e) {
		}
		try {
			if (workbook != null) {
				workbook.close();
			}
		} catch (IOException e) {
		}
	}
}
static void generateFinalReport(Map<String, List<String>> finalReport2) {
	
	File file = null;
	OutputStream fos = null;
	try {
		file = new File(filename);
	if (file.exists()) {
		workbk = (HSSFWorkbook) WorkbookFactory.create(new FileInputStream(filename));
	} else {
		workbk = new HSSFWorkbook();
	}
	for (Map.Entry<String, List<String>> entry : finalReport2.entrySet()) {
	if(workbk.getSheet("Event Details") == null) {
		
		lastSheet = workbk.createSheet("Event Details");
		List<String> fieldNames = new ArrayList<String>();
		fieldNames.add("Interface");
		fieldNames.add("Minimum");
		fieldNames.add("Maximum");
		fieldNames.add("Average");
		row = lastSheet.createRow(rwCnt++);
		int columnCount = 0;
		for (String fieldName : fieldNames) {
			Cell cell = row.createCell(columnCount++);
			cell.setCellValue(fieldName);
		}
	}else{
		lastSheet = workbk.getSheet("Event Details");
	}
	
	List<String> eventDetails = entry.getValue();
    row = lastSheet.createRow(rwCnt++);
    if(!eventDetails.isEmpty()) {
	    Cell cell0 = row.createCell(0);
		cell0.setCellValue(entry.getKey());
		Cell cell1 = row.createCell(1);
		cell1.setCellValue((String) eventDetails.get(0));
		Cell cell2 = row.createCell(2);
		cell2.setCellValue("( "+(String) eventDetails.get(2)+" ) "+(String) eventDetails.get(1));
		Cell cell3 = row.createCell(3);
		cell3.setCellValue((String) eventDetails.get(3));
    }
	fos = new FileOutputStream(file);
	workbk.write(fos);
	fos.flush();
	}
} catch (Exception e) {
	e.printStackTrace();
} finally {
	try {
		if (fos != null) {
			fos.close();
		}
	} catch (IOException e) {
	}
	try {
		if (workbk != null) {
			workbk.close();
		}
	} catch (IOException e) {
	}
}
}
static String averageOfEvent(TreeMap<Double,String> eventDetails) {
	Double sum = 0.0d;
	Double avg = 0.0d;
	if(!eventDetails.isEmpty()) {
		for (Map.Entry<Double, String> entry : eventDetails.entrySet()) {
			sum += entry.getKey(); 
		}
		avg = sum / eventDetails.size();
    }
 return new DecimalFormat("#0.00").format(avg);
}
}
