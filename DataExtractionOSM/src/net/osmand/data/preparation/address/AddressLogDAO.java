package net.osmand.data.preparation.address;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import net.osmand.data.Boundary;
import net.osmand.data.City;
import net.osmand.data.preparation.AbstractIndexPartCreator;
import net.osmand.data.preparation.DBDialect;
import net.osmand.osm.Entity;
import net.osmand.osm.Entity.EntityType;
import net.osmand.osm.Node;
import net.osmand.osm.Relation;
import net.osmand.osm.Way;

import org.apache.commons.logging.Log;

public class AddressLogDAO extends AbstractIndexPartCreator {
	
	private final Log log;

	private PreparedStatement insertLog;
	private PreparedStatement selectLog;
	
	public AddressLogDAO(Log log) {
		this.log = log;
	}

	public void createDatabaseStructure(Connection mapConnection, DBDialect dialect) throws SQLException {
		Statement stat = mapConnection.createStatement();
        stat.executeUpdate("create table address_log (id INTEGER PRIMARY KEY autoincrement, entityid bigint, streetname varchar(100), entitytype int, reason varchar(1024))");
        stat.close();
        
        if (insertLog == null) {
        	insertLog = createPrepareStatement(mapConnection,"insert into address_log (entityid, streetname, entitytype, reason) values (?, ?, ?, ?)");
        }
        if (selectLog == null) {
        	selectLog = createPrepareStatement(mapConnection,"select * from address_log");
        }
	}

	public void boundaryEvent(Boundary boundary, String reason) 
	{
		log(boundary.getBoundaryId(), null, Entity.EntityType.RELATION, reason);
	}

	private void log(long entityID, String street, EntityType entityType, String reason) {
		try {
			log.info("Entityid:" + entityID + " street: " + street + " reason: " + reason);
			insertLog.setLong(1, entityID);
			insertLog.setString(2, street);
			insertLog.setInt(3, entityType != null ? entityType.ordinal() : -1);
			insertLog.setString(4, reason);
			addBatch(insertLog);
		} catch (SQLException e) {
			//do nothing...
		}
	}

	public void cityEvent(City cityFound, String string) {
		log(cityFound.getId(), null, cityFound.getEntityId().getType(), string);
	}

	public void streetEvent(Relation i, String street, String string) {
		log(i.getId(), street, EntityType.RELATION, string);
	}

	public void streetEvent(String name, String string) {
		log(-1, name, null, string);
	}

	public void houseEvent(Relation i, String street, String string) {
		log(i.getId(), street, EntityType.RELATION, string);
	}

	public void houseEvent(Way e, String string) {
		log(e.getId(), null, EntityType.WAY, string);
	}

	public void houseEvent(Node first, String string) {
		log(first.getId(), null, EntityType.NODE, string);
	}

	public void houseEvent(Entity e, String string) {
		log(e.getId(), null, EntityType.valueOf(e), string);
	}

	public void close() {
		try {
			closeAllPreparedStatements();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void writeAddressLog(String addressLogFileName) {
		//create new sqlite db and copy the log there!
		try {
			Connection mapConnection = (Connection) DBDialect.SQLITE.getDatabaseConnection(addressLogFileName, log);
			createDatabaseStructure(mapConnection, DBDialect.SQLITE);
			
			ResultSet resultSet = selectLog.executeQuery();
			PreparedStatement ins = mapConnection.prepareStatement("insert into address_log (id, entityid, streetname, entity, reason) values (?, ?, ?, ?, ?)");
			while (resultSet.next()) {
				ins.setInt(1, resultSet.getInt(1));
				ins.setLong(2, resultSet.getLong(2));
				ins.setString(3, resultSet.getString(3));
				ins.setInt(4, resultSet.getInt(4));
				ins.setString(5, resultSet.getString(5));
				ins.executeUpdate();
			}
			//TODO finally ??
			ins.close();
			resultSet.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}


}
