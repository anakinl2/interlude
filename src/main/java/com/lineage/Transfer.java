package com.lineage;

import com.lineage.db.DatabaseUtils;
import com.lineage.db.FiltredPreparedStatement;
import com.lineage.db.L2DatabaseFactory;
import com.lineage.db.ThreadConnection;
import com.mchange.v2.c3p0.ComboPooledDataSource;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * author: Vadim
 */
public class Transfer {

    public static void main(String[] args) throws SQLException, PropertyVetoException {

        Server.SERVER_MODE=Server.MODE_GAMESERVER;
        Config.loadAllConfigs();
        ComboPooledDataSource comboPooledDataSource = L2DatabaseFactory.getInstance().getDataSource("jdbc:mysql://localhost/gameserver_beta", "root", "root");

        Connection connection=comboPooledDataSource.getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM char_templates");
        ResultSet charTemplates = preparedStatement.executeQuery();
        while (charTemplates.next()) {
            int classId = charTemplates.getInt("ClassId");
            int x = charTemplates.getInt("x");
            int y = charTemplates.getInt("y");
            int z = charTemplates.getInt("z");
            int items1 = charTemplates.getInt("items1");
            int items2 = charTemplates.getInt("items2");
            int items3 = charTemplates.getInt("items3");
            int items4 = charTemplates.getInt("items4");
            int items5 = charTemplates.getInt("items5");


            ThreadConnection con=L2DatabaseFactory.getInstance().getConnection();
            FiltredPreparedStatement preparedStatement1 = con.prepareStatement("UPDATE char_templates SET x=?, y=?, z=?, items1=?,items2=?,items3=?,items4=?,items5=? WHERE ClassId=?");

            preparedStatement1.setInt(9, classId);

            preparedStatement1.setInt(1, x);
            preparedStatement1.setInt(2, y);
            preparedStatement1.setInt(3, z);
            preparedStatement1.setInt(4, items1);
            preparedStatement1.setInt(5, items2);
            preparedStatement1.setInt(6, items3);
            preparedStatement1.setInt(7, items4);
            preparedStatement1.setInt(8, items5);
            preparedStatement1.executeUpdate();
            DatabaseUtils.closeDatabaseCS(con, preparedStatement1);
        }
        charTemplates.close();
        preparedStatement.close();
    }
}
