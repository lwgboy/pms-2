/**   
 * @ClassName:     ${UserUploadService}   
 * @Description:   ${用户数据上传管理功能}   
 * 
 * @ProductName:   ${中盈集中用户平台}
 * @author         ${北京中盈网信科技有限公司}  
 * @version        V1.0     
 * @Date           ${2014.9.8} 
*/
package com.pms.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;


import com.pms.dao.AuditLogDAO;
import com.pms.dao.AuditLogDescribeDao;
import com.pms.dao.OrganizationDAO;
import com.pms.dao.UserDAO;
import com.pms.dao.impl.AuditLogDAOImpl;
import com.pms.dao.impl.AuditLogDescribeDAOImpl;
import com.pms.dao.impl.OrganizationDAOImpl;
import com.pms.dao.impl.UserDAOImpl;
import com.pms.model.AuditUserLog;
import com.pms.model.AuditUserLogDescribe;
import com.pms.model.Organization;
import com.pms.model.User;
import com.pms.model.UserImport;
import com.pms.util.DateTimeUtil;
import com.pms.util.MD5Security;

public class UserUploadService {
	
	private static Log logger = LogFactory.getLog(UserUploadService.class);
	
	private final String SHEET_USER = "批量导出";
	
	private final String SHEET_USER_NAME = "姓名";
	private final String SHEET_USER_ORGID = "组织机构";
	private final String SHEET_USER_USERCODE = "用户编号";
	private final String SHEET_USER_IDCARD = "身份证";
	private final String SHEET_USER_SEX = "性别";
	private final String SHEET_USER_POSITION = "岗位";
	private final String SHEET_USER_POLICESORT = "警种";
	private final String SHEET_USER_TAKE_OFFICE = "职级";
	private final String SHEET_USER_OFFICELEVEL = "任职";
	
	public void UploadUser(File inData) throws Exception {
		InputStream in=new FileInputStream(inData);
        Workbook workbook = WorkbookFactory.create(in);
        
        int sheetCount = workbook.getNumberOfSheets();  //Sheet的数量  
        //deal with seven elements and relationships
        for (int s = 0; s < sheetCount; s++) {
            Sheet sheet = workbook.getSheetAt(s);
            String sheetName = sheet.getSheetName();
            if ( SHEET_USER.equals(sheetName) ) {
            	updateUserImport(sheet);
            }
        }
        
        in.close();
        
        //update organization;
        updateUser();
        AddUserImportLog();
        
        return;
	}
	
	private void updateUser() throws Exception {
		UserDAO udao = new UserDAOImpl();
		List<UserImport> imports = udao.GetUserImports();
		
		OrganizationDAO odao = new OrganizationDAOImpl();
		Organization org = null;
		for(int i = 0; i < imports.size(); i++) {
			UserImport ui = imports.get(i);
			org = odao.GetOrgNodeById(ui.getGA_DEPARTMENT());
			if(org != null) {
				udao.UserImport(ui, org);
			}
			else {
				String warnMsg = "[IU]用户身所属机构不存在。[用户名:" + ui.getNAME() + ", 身份证号:" + ui.getCERTIFICATE_CODE_MD5() + ", 机构编号:" + ui.getGA_DEPARTMENT() + "]";
    			logger.warn(warnMsg);
			}
		}
		
		return;
	}

	private void updateUserImport(Sheet sheet) throws Exception {
		int rowCount = sheet.getPhysicalNumberOfRows(); //获取总行数
		Map<String, Integer> idx = new HashMap<String, Integer>();
		UserImport ui = null;
		UserDAO dao = new UserDAOImpl();
		dao.UserImportClear();
		
		//遍历每一行 
        for (int r = 0; r < rowCount; r++) {
        	Row row = sheet.getRow(r);
        	if(row == null) {
        		continue;
        	}
        	if( r > 2 ) {
        		ui = new UserImport();
        	}
        	int cellCount = row.getPhysicalNumberOfCells(); //获取总列数 
        	cellCount = row.getLastCellNum();
        	//遍历每一列  
            for (int c = 0; c < cellCount; c++) {
            	Cell cell = row.getCell(c);
            	String cellValue = getCellValue(cell);
            	
            	if(r == 2) {
            		if ( SHEET_USER_NAME.equals(cellValue) ) {
            			idx.put(SHEET_USER_NAME, c);
            		} else if ( SHEET_USER_ORGID.equals(cellValue) ) {
            			idx.put(SHEET_USER_ORGID, c);
            		} else if ( SHEET_USER_USERCODE.equals(cellValue) ) {
            			idx.put(SHEET_USER_USERCODE, c);
            		} else if ( SHEET_USER_IDCARD.equals(cellValue) ) {
            			idx.put(SHEET_USER_IDCARD, c);
            		} else if ( SHEET_USER_SEX.equals(cellValue) ) {
            			idx.put(SHEET_USER_SEX, c);
            		} else if ( SHEET_USER_POSITION.equals(cellValue) ) {
            			idx.put(SHEET_USER_POSITION, c);
            		} else if ( SHEET_USER_POLICESORT.equals(cellValue) ) {
            			idx.put(SHEET_USER_POLICESORT, c);
            		} else if ( SHEET_USER_TAKE_OFFICE.equals(cellValue) ) {
            			idx.put(SHEET_USER_TAKE_OFFICE, c);
            		} else if ( SHEET_USER_OFFICELEVEL.equals(cellValue) ) {
            			idx.put(SHEET_USER_OFFICELEVEL, c);
            		}
            	} else if(r > 2) {
            		if(idx.size() == 0) {
            			throw new Exception("导入数据文件格式不正确!");
            		}
            		if( c == idx.get(SHEET_USER_NAME) ) {
            			ui.setNAME(cellValue);
            		} else if ( c == idx.get(SHEET_USER_ORGID) ) {
            			ui.setGA_DEPARTMENT(cellValue);
            		} else if ( c== idx.get(SHEET_USER_USERCODE) ) {
            			//user code equals idcard
            		} else if ( c== idx.get(SHEET_USER_IDCARD) ) {
            			if( cellValue == null || (cellValue.length() != 15 && cellValue.length() != 18) ) {
            				String warnMsg = "[IU]用户身份证号格式不正确。[用户名:" + ui.getNAME() + ", 身份证号:" + cellValue + "]";
                			logger.warn(warnMsg);
            				break;
            			}
            			String md5 = getIDCardHash(cellValue);
            			
            			ui.setCERTIFICATE_CODE_MD5(md5);
            			ui.setCERTIFICATE_CODE_SUFFIX(cellValue.substring(cellValue.length() > 6 ? cellValue.length() - 6 : 0));
            		} else if ( c== idx.get(SHEET_USER_SEX) ) {
//						String sex = null;
//            			if(cellValue == null || cellValue.isEmpty()) {
//            				sex = User.USER_SEXCODE_UNKNOWN;
//            			}
//            			else {
//            				if( "男".equals(cellValue) ) {
//                				sex = User.USER_SEXCODE_MALE;
//                			}
//                			else if( "女".equals(cellValue) ) {
//                				sex = User.USER_SEXCODE_FEMALE;
//                			}
//                			else {
//                				sex = User.USER_SEXCODE_UNSPOKEN;
//                			}
//            			}
//            			ui.setSEXCODE(sex);
            			String sex = User.GetSexCode(cellValue);
            			if(sex == null) {
            				sex = cellValue;
            			}
            			ui.setSEXCODE(sex);
            		} else if ( c== idx.get(SHEET_USER_POSITION) ) {
            			ui.setPosition(cellValue);
            		} else if ( c== idx.get(SHEET_USER_POLICESORT) ) {
            			String sort = User.GetPoliceSortCode(cellValue);
            			if(sort == null) {
            				sort = cellValue;
            			}
            			ui.setPOLICE_SORT(sort);
            		} else if ( c== idx.get(SHEET_USER_TAKE_OFFICE) ) {
            			ui.setTAKE_OFFICE(cellValue);
            		} else if ( c== idx.get(SHEET_USER_OFFICELEVEL) ) {
            			ui.setOfficelevel(cellValue);
            		}
            	}
            }
            
            if( r > 2 ) {
            	if(ui.isValid()) {
		            dao.UserImportSave(ui);
            	}
            }
        }
		return;
	}
	
	private String getCellValue(Cell cell) {
		if(cell == null) {
			return "";
		}
		int cellType = cell.getCellType();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String cellValue = null;  
        switch(cellType) {
            case Cell.CELL_TYPE_STRING: //文本  
                cellValue = cell.getStringCellValue();  
                break;  
            case Cell.CELL_TYPE_NUMERIC: //数字、日期  
                if(DateUtil.isCellDateFormatted(cell)) {  
                    cellValue = sdf.format(cell.getDateCellValue()); //日期型  
                }  
                else {  
                    cellValue = String.valueOf(cell.getNumericCellValue()); //数字  
                    if(cellValue.contains(".")) {
                    	cellValue = cellValue.substring(0, cellValue.indexOf('.'));
                    }
                }  
                break;  
            case Cell.CELL_TYPE_BOOLEAN: //布尔型  
                cellValue = String.valueOf(cell.getBooleanCellValue());  
                break;  
            case Cell.CELL_TYPE_BLANK: //空白  
                cellValue = cell.getStringCellValue();  
                break;  
            case Cell.CELL_TYPE_ERROR: //错误  
                cellValue = "错误";  
                break;  
            case Cell.CELL_TYPE_FORMULA: //公式  
                cellValue = "错误";  
                break;  
            default:  
                cellValue = "错误";  
        }
        return cellValue;
	}
	
	private String getIDCardHash(String idcard) throws Exception {
		return MD5Security.md5(idcard);
	}
	
	private void AddUserImportLog() throws Exception {
		String timenow = DateTimeUtil.GetCurrentTime();
		
		AuditUserLog auditUserLog = new AuditUserLog();
		AuditLogDAO logdao = new AuditLogDAOImpl();
		AuditLogService als = new AuditLogService();
		
		auditUserLog.setAdminId(als.adminLogin());
		auditUserLog.setIpAddr("");
		auditUserLog.setFlag(AuditUserLog.LOGFLAGIMPORT);
		auditUserLog.setResult(AuditUserLog.LOGRESULTSUCCESS);
		auditUserLog.setLATEST_MOD_TIME(timenow);
		auditUserLog = logdao.AuditUserLogAdd(auditUserLog);
		
		AuditUserLogDescribe auditUserLogDescribe = new AuditUserLogDescribe();
		AuditLogDescribeDao logDescdao = new AuditLogDescribeDAOImpl();
		
		auditUserLogDescribe.setLogid(auditUserLog.getId());
		
		String str="";
		str += "用户导入";
		
		auditUserLogDescribe.setDescrib(str);
		
		auditUserLogDescribe.setLATEST_MOD_TIME(timenow);
		auditUserLogDescribe = logDescdao.AuditUserLogDescribeAdd(auditUserLogDescribe);
	}
}
