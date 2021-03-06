package com.m_mb.mm.db;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.mysql.jdbc.CallableStatement;

public class DB {
	
	//region Properties...
	
	private String ConnectionString = "";
	
	//endregion
	
	//region Constructors...
	
	public DB(String server, String database, String username, String password, String port) {
		if (port == null || port.isEmpty()) {
			port = "3306";
		}
		ConnectionString = "jdbc:mysql://" + server + ":" + port + "/" + database + "?" + "user=" + username + "&password=" + password; 
	}
	
	public DB(String server, String database, String username, String password) {
		String port = "3306";
		ConnectionString = "jdbc:mysql://" + server + ":" + port + "/" + database + "?" + "user=" + username + "&password=" + password; 
	}
	
	//endregion
	
	//region Helpers...
	
	private static <T> void SetStatementParameters(CallableStatement statement, Map<String, Object> properties) throws Exception {
		for (Map.Entry<String, Object> item : properties.entrySet()) {
    		String propertyName = item.getKey();
    		Object value = item.getValue();
    		try {
	    		if (value != null) {
	    			if (value instanceof String) {
	        			statement.setString(propertyName, value.toString());	
	        		} else if (value instanceof Integer) {
	        			Integer v = Integer.parseInt(value.toString());
	        			statement.setInt(propertyName, v);
	        		} else if (value instanceof Boolean) {
	        			Boolean v = Boolean.parseBoolean(value.toString());
	        			statement.setBoolean(propertyName, v);
	        		}
	    		}
    		} catch (Exception e) {
    			System.out.println(propertyName + "=" + e.getMessage());
    		}
    	}
	}
	
	private static String CreateParameterString(int count) {
		String text = "";
		for (int i = 0; i < count; i++) {
	    	if (i == count - 1) {
	    		text += "?";
	    	} else {
	    		text += "?,";	
	    	}
	    }
		System.out.println(text);
		return text;
	}
	
	private static <T> void SetStatementParameters(CallableStatement statement, List<Field> properties, T data) throws Exception {
		for (Field field : properties) {
    		String propertyName = field.getName();
    		Object value = field.get(data);
    		try {
	    		if (value != null) {
	    			if (value instanceof String) {
	        			statement.setString(propertyName, value.toString());	
	        		} else if (value instanceof Integer) {
	        			Integer v = Integer.parseInt(value.toString());
	        			statement.setInt(propertyName, v);
	        		} else if (value instanceof Boolean) {
	        			Boolean v = Boolean.parseBoolean(value.toString());
	        			statement.setBoolean(propertyName, v);
	        		} else if (value instanceof byte[]) {
	        			statement.setBytes(propertyName, (byte[]) value);
	        		}
	    		}
    		} catch (Exception e) {
    			System.out.println(propertyName + "=" + e.getMessage());
    		}
    	}
	}
	
	private static Field GetPrimaryKey(Field[] properties) {
		Field primaryKey = null;
		for (Field field : properties) {
    		Annotation[] ano = field.getDeclaredAnnotations();
    		for (Annotation a : ano) {
    			if (a instanceof PrimaryKey) {
    				primaryKey = field;
    				break;
    			}
    		}
    	}
		return primaryKey;
	}
	
	private static List<Field> GetDataParameters(Field[] properties, boolean includePrimaryKey) {
		List<Field> allDPS = new ArrayList<Field>();
		for (Field field : properties) {
    		boolean IsDataParameter = false;
    		boolean IsPrimaryKey = false;
    		Annotation[] ano = field.getDeclaredAnnotations();
    		for (Annotation a : ano) {
    			if (a instanceof DataParameter) {
    				IsDataParameter = true;
    			}
    			if (a instanceof PrimaryKey) {
    				IsPrimaryKey = true;
    			}
    		}
    		if (includePrimaryKey && IsPrimaryKey) {
    			allDPS.add(field);
    		} else if (IsDataParameter && !IsPrimaryKey) {
    			allDPS.add(field);
    		}
    	}
		return allDPS;
	}
	
	private void SetBytes(Object object, Object value, Field field) {
		try {
			field.set(object, value);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void SetEnum(Object object, Object value, Field field) {
		try {
			Class enumType = field.getType();
			Enum eValue = Enum.valueOf(enumType, (String) value);
			field.set(object, eValue);
		} catch (Exception e) {
			System.out.print(e.getMessage());
		}
	}
	
	private void SetInt(Object object, Object value, Field field) {
		try {
			Integer intValue = Integer.parseInt(value.toString());
			field.set(object, intValue);
		} catch (Exception e) {
			System.out.print(e.getMessage());
		}
	}

	private void SetString(Object object, Object value, Field field) {
		try {
			field.set(object, value);
		} catch (Exception e) {
			System.out.print(e.getMessage());
		}
	}
	
	private void SetBool(Object object, Object value, Field field) {
		try {
			boolean bValue = false;
			if (value.toString().equals("1")) {
				bValue = true;
			}
			field.set(object, bValue);
		} catch (Exception e) {
			System.out.print(e.getMessage());
		}
	}
	
	//endregion
	
	//region Add...
	
	public <T> int Add(T data, String procedure, boolean includePrimaryKey) throws Exception {
		Class.forName("com.mysql.jdbc.Driver");
	    Connection connect = DriverManager.getConnection(ConnectionString);
	    Field[] fields = data.getClass().getFields();
	    List<Field> properties = GetDataParameters(fields, includePrimaryKey);
	    System.out.println("p=" + properties.size());
	    CallableStatement statement = (CallableStatement) connect.prepareCall("{call " + procedure + "(" + CreateParameterString(properties.size()) + ")}");
	    SetStatementParameters(statement, properties, data);
    	Field primaryKey = GetPrimaryKey(fields);
    	ResultSet rs = statement.executeQuery();
    	int id = 0;
    	while (rs.next()) {
    		if (primaryKey != null) {
        		id = rs.getInt(primaryKey.getName());
        	}	
    	}
		return id;
	}
	
	//endregion
	
	//region Update...
	
	public <T> void Update(Map<String, Object> data, String procedure) {
    	for (Map.Entry<String, Object> item : data.entrySet()) {
    		String propertyName = item.getKey();
    		Object value = item.getValue();
    		System.out.println(value.toString());
    		System.out.println(propertyName);
    	}
	}
	
	public <T> void Update(T data, String procedure) throws Exception {
		List<Field> properties = GetDataParameters(data.getClass().getFields(), true);
    	Class.forName("com.mysql.jdbc.Driver");
	    Connection connect = DriverManager.getConnection(ConnectionString);	    
	    CallableStatement statement = (CallableStatement) connect.prepareCall("{call " + procedure + "(" + CreateParameterString(properties.size()) + ")}");
	    SetStatementParameters(statement, properties, data);
    	statement.execute();
	}
	
	//endregion
	
	//region Remove...
	
	public <T> void Remove(Map<String, Object> data, String procedure) throws Exception {
		Class.forName("com.mysql.jdbc.Driver");
	    Connection connect = DriverManager.getConnection(ConnectionString);	    
	    CallableStatement statement = (CallableStatement) connect.prepareCall("{call " + procedure + "(" + CreateParameterString(data.size()) + ")}");
	    SetStatementParameters(statement, data);
    	statement.execute();
	}
	
	@SuppressWarnings("unused")
	public <T> void Remove(T data, String Procedure) throws IllegalArgumentException, IllegalAccessException {
		Field[] properties = data.getClass().getFields();
    	for (int i = 0; i < properties.length;i++) {
    		String propertyName = properties[i].getName();
    		Object value = properties[i].get(data);
    	}
	}
	
	//endregion
	
	//region Read...
	
	@SuppressWarnings("unchecked")
	public <T> List<T> ReadCollection(Class<T> type, String procedure) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
	    Class.forName("com.mysql.jdbc.Driver");
	    Connection connect = DriverManager.getConnection(ConnectionString);
	    CallableStatement statement = (CallableStatement) connect.prepareCall("{call " + procedure + "}");
	    List<Field> properties = new ArrayList<Field>();
	    for (Field field : type.newInstance().getClass().getDeclaredFields()) {
	    	Annotation[] annotations = field.getDeclaredAnnotations();
			boolean isDataParameter = false;
			for (Annotation a : annotations) {
				if (a instanceof DataParameter) {
					isDataParameter = true;
				}
			}
			if (isDataParameter) {
				properties.add(field);	
			}
	    }
	    boolean success = statement.execute();
	    List<T> list = new ArrayList<T>();
	    if (success) {
	    	ResultSet resultSet = statement.getResultSet();
		    while (resultSet.next()) {
		    	Object item = type.newInstance();
		    	for (Field field : properties) {
		    		String propertyName = field.getName();
	    			if (field.getType() == int.class) {
	    				Object value = resultSet.getString(propertyName);
	    				SetInt(item, value, field);
	    			} else if (field.getType() == boolean.class) {
	    				Object value = resultSet.getString(propertyName);
	    				SetBool(item, value, field);
	    			} else if (field.getType() instanceof Class && ((Class<?>)field.getType()).isEnum()) {
	    				Object value = resultSet.getString(propertyName);
	    				SetEnum(item, value, field);
	  				} else if (field.getType() == String.class) {
	  					Object value = resultSet.getString(propertyName);
	  					SetString(item, value, field);
	  				} else if (field.getType() == byte[].class) {
	  					byte[] value = resultSet.getBytes(propertyName);
	  					SetBytes(item, value, field);
	  				}
		    	}
		    	list.add((T) item);
			}
	    }
	    return (List<T>) list;
	}
	
	//endregion

}