package com.pms.service;

import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import sun.misc.BASE64Encoder;

import com.pms.dao.AttributeDAO;
import com.pms.dao.AuditLogDAO;
import com.pms.dao.AuditLogDescribeDao;
import com.pms.dao.PrivilegeDAO;
import com.pms.dao.UserDAO;
import com.pms.dao.impl.AttributeDAOImpl;
import com.pms.dao.impl.AuditLogDAOImpl;
import com.pms.dao.impl.AuditLogDescribeDAOImpl;
import com.pms.dao.impl.PrivilegeDAOImpl;
import com.pms.dao.impl.UserDAOImpl;
import com.pms.dto.PrivUserListItem;
import com.pms.dto.UserListItem;
import com.pms.model.AttrDictionary;
import com.pms.model.AuditUserLog;
import com.pms.model.AuditUserLogDescribe;
import com.pms.model.Organization;
import com.pms.model.Privilege;
import com.pms.model.User;



public class UserManageService {
	
	public User SaveUser( User user ) throws Exception
	{
		UserDAO dao = new UserDAOImpl();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
				Locale.SIMPLIFIED_CHINESE);
		String timenow = sdf.format(new Date());
		
		user.setLATEST_MOD_TIME(timenow);
		user.setDATA_VERSION(user.getDATA_VERSION()+1);
		String idNum = user.getCERTIFICATE_CODE_SUFFIX();
		if( idNum.length() > 6 ){
			user.setCERTIFICATE_CODE_MD5( generateHash(idNum) );
			user.setCERTIFICATE_CODE_SUFFIX( generateSuffix(idNum) );
		}
		AddUserAddOrUpdateLog(user);
		
		user = dao.UserAdd(user);
		
		return user;
	}
	
	public int QueryUserItems(String pid, int page, int rows,
			List<UserListItem> items) throws Exception {
		UserDAO dao = new UserDAOImpl();
		List<User> res = dao.GetUsersByParentId( pid, page, rows );
		UserListItem userItem = null;
		for(int i=0; i<res.size(); i++) {
			userItem = ConvertUserToListItem(res.get(i));
			items.add(userItem);
		}
		int total = QueryChildrenUsersCount( pid, null );
		return total;
	}
	
	public int QueryChildrenUsersCount(String pid, User criteria) throws Exception {
		UserDAO dao = new UserDAOImpl();
		int count = dao.GetUsersCountByParentId( pid, criteria );
		return count;
	}

	public UserListItem ConvertUserToListItem(User user) throws Exception {
		UserListItem item = new UserListItem();
		item.setId(user.getId());
		item.setName(user.getNAME());
		item.setParent_id(user.getGA_DEPARTMENT());
		item.setOrgLevel(user.getORG_LEVEL());
		item.setDept(user.getDept());
		item.setIdnum(user.getCERTIFICATE_CODE_SUFFIX());
		item.setMax_sensitive_level(user.getSENSITIVE_LEVEL());
		item.setBusiness_type(user.getBUSINESS_TYPE());
		item.setPolice_num(user.getPOLICE_NO());
		item.setPolice_type(user.getPOLICE_SORT());
		item.setPosition(user.getPosition());
		item.setSex(user.getSEXCODE());
		item.setStatus(user.getUSER_STATUS());
		item.setTitle(user.getTAKE_OFFICE());
		item.setData_version(user.getDATA_VERSION());
		item.setCertificate_code_md5(user.getCERTIFICATE_CODE_MD5());
		
		OrgManageService oms = new OrgManageService();
		String path = oms.QueryNodePath(user.getGA_DEPARTMENT());
		if(path != null && path.length() > 0){
			//name1/name2/name3/name4 -->  name1/name2/name3  and  name4
			int index = path.lastIndexOf('/');
			String pname = "";
			String gname = "";
			if( -1 == index ) {
				pname = path;
			}
			else {
				pname = path.substring(path.lastIndexOf('/')+1, path.length());
				gname = path.substring(0, path.lastIndexOf('/'));
			}
			item.setPname(pname);
			item.setGname(gname);
		}
		
		AttributeDAO attrdao = new AttributeDAOImpl();
		List<AttrDictionary> attrDicts = attrdao.GetUsersDictionarys(user.getId());
		List<AttrDictionary> data = new ArrayList<AttrDictionary>();
		for(int i = 0; i < attrDicts.size(); i++) {
			AttrDictionary attrDictionary=new AttrDictionary();

			attrDictionary.setId(attrDicts.get(i).getId());
			attrDictionary.setAttrid(attrDicts.get(i).getAttrid());
			attrDictionary.setValue(attrDicts.get(i).getValue());
			attrDictionary.setCode(attrDicts.get(i).getCode());
			attrDictionary.setTstamp(attrDicts.get(i).getTstamp());
			data.add(attrDictionary);
		}
		
		item.setDictionary(data);
		
		return item;
	}

	public int QueryAllUserItems(String pid, User criteria, int page, int rows,
			List<UserListItem> items) throws Exception {
		criteria.setDELETE_STATUS(User.DELSTATUSNO);
		UserDAO dao = new UserDAOImpl();
		int total = 0;
		List<User> res = new ArrayList<User>();
		UserListItem userItem = null;
		
		if (pid.equals("0")) {
			res=dao.GetAllUsers(criteria, page, rows);
			for(int i=0; i<res.size(); i++) {
				userItem = ConvertUserToListItem(res.get(i));
				items.add(userItem);
			}
			total=dao.GetAllUsersCount(criteria);
		}else{
			//get all orgs;
			List<Organization> nodes = new ArrayList<Organization>();
			OrgManageService oms = new OrgManageService();
			oms.queryAllChildrenNodesById(pid, null, nodes);
			Organization first = new Organization();
			first.setGA_DEPARTMENT(pid);
			nodes.add(0, first);
		
			//calculate all items' count and get users;
			int pre_count = 0;
			for(int i = 0; i< nodes.size(); i++) {
				total += QueryChildrenUsersCount( nodes.get(i).getGA_DEPARTMENT(), criteria );
				if( total <= ((page-1) * rows) ) {
					pre_count = total;
				}
				else if ( total <= (page * rows) ) {
					List<User> tmp = dao.GetUsersByParentIdWithNoPage( nodes.get(i).getGA_DEPARTMENT(), criteria );
					if( tmp != null && tmp.size() > 0) {
						if( pre_count < ((page-1) * rows) ) {
							int fromIndex = rows - (pre_count%rows);
							int toIndex = tmp.size() - fromIndex >= rows ? fromIndex+rows : tmp.size();
							res.addAll(tmp.subList( fromIndex, toIndex));
						}
						else {
							int toIndex = rows - (pre_count%rows) >= tmp.size() ? tmp.size() : rows - (pre_count%rows);
							res.addAll(tmp.subList(0, toIndex));
						}
					}
					pre_count = total;
				} 
				else {//total > page * rows
					if( pre_count < (page * rows) ) {
						List<User> tmp = dao.GetUsersByParentIdWithNoPage( nodes.get(i).getGA_DEPARTMENT(), criteria );
						if( tmp != null && tmp.size() > 0 ) {
							int toIndex =  rows - (pre_count%rows);
							res.addAll(tmp.subList( 0, toIndex));
						}
					}
					pre_count = total;
				}
			}
			
			//convert to datagrid's item
			for(int i=0; i<res.size(); i++) {
				userItem = ConvertUserToListItem(res.get(i));
				items.add(userItem);
			}
		}
		
		AddUserQueryLog(criteria);
		
		return total;
	}

	public int QueryAllPrivUserItems(String pid, int privStatus, User criteria, int page, int rows,
			List<PrivUserListItem> items) throws Exception {
		List<UserListItem> ulItems = new ArrayList<UserListItem>();
		int total = QueryAllUserItems(pid, criteria, page, rows, ulItems);
		
		PrivilegeDAO pdao = new PrivilegeDAOImpl();
		for(int i = 0; i<ulItems.size(); i++) {
			PrivUserListItem pulItem = new PrivUserListItem();
			pulItem.setId(ulItems.get(i).getId());
			pulItem.setName(ulItems.get(i).getName());
			pulItem.setParent_id(ulItems.get(i).getParent_id());
			pulItem.setPname(ulItems.get(i).getPname());
			pulItem.setGname(ulItems.get(i).getGname());
			pulItem.setStatus(ulItems.get(i).getStatus());
			
			int count = pdao.QueryPrivilegesCountByOwnerId(ulItems.get(i).getId(), Privilege.OWNERTYPEUSER);
			pulItem.setPriv_status(count>0?PrivUserListItem.PRIVSTATUSYES:PrivUserListItem.PRIVSTATUSNO);
			if(PrivUserListItem.PRIVSTATUSYES == privStatus) {
				if( pulItem.getPriv_status() == PrivUserListItem.PRIVSTATUSNO ) {
					total--;
					continue;
				}
			} else if( PrivUserListItem.PRIVSTATUSNO == privStatus) {
				if( pulItem.getPriv_status() == PrivUserListItem.PRIVSTATUSYES ) {
					total--;
					continue;
				}
			}
			
			items.add(pulItem);
		}
		return total;
	}

	public int QueryPrivUserItems(String pid, int privStatus, int page, int rows,
			List<PrivUserListItem> items) throws Exception {
		List<UserListItem> ulItems = new ArrayList<UserListItem>();
		int total = QueryUserItems(pid, page, rows, ulItems);

		PrivilegeDAO pdao = new PrivilegeDAOImpl();
		for(int i = 0; i<ulItems.size(); i++) {
			PrivUserListItem pulItem = new PrivUserListItem();
			pulItem.setId(ulItems.get(i).getId());
			pulItem.setName(ulItems.get(i).getName());
			pulItem.setParent_id(ulItems.get(i).getParent_id());
			pulItem.setPname(ulItems.get(i).getPname());
			pulItem.setGname(ulItems.get(i).getGname());
			pulItem.setStatus(ulItems.get(i).getStatus());
			
			int count = pdao.QueryPrivilegesCountByOwnerId(ulItems.get(i).getId(), Privilege.OWNERTYPEUSER);
			pulItem.setPriv_status(count>0?PrivUserListItem.PRIVSTATUSYES:PrivUserListItem.PRIVSTATUSNO);
			items.add(pulItem);
		}
		return total;
	}
	
	public void DeleteUserNodes(List<Integer> nodeIds) throws Exception
	{	
		if(nodeIds == null)
			return;
		User user;
		for(int i = 0; i< nodeIds.size(); i++) {
			user = new User();
			user.setId(nodeIds.get(i));
			
			DeleteUserNode(user);
			
			AddUserDelLog(user);
		}
		
		return ;
	}
	public User DeleteUserNode(User user) throws Exception
	{
		UserDAO dao = new UserDAOImpl();
		List<User> nodes = dao.GetUserById(user.getId());
	
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
				Locale.SIMPLIFIED_CHINESE);
		String timenow = sdf.format(new Date());
		
		for(int i = 0; i< nodes.size(); i++) {
			user.setNAME(nodes.get(i).getNAME());
			user.setCERTIFICATE_CODE_MD5(nodes.get(i).getCERTIFICATE_CODE_MD5());
			user.setCERTIFICATE_CODE_SUFFIX(nodes.get(i).getCERTIFICATE_CODE_SUFFIX());
			user.setSEXCODE(nodes.get(i).getSEXCODE());
			user.setGA_DEPARTMENT(nodes.get(i).getGA_DEPARTMENT());
			user.setUNIT(nodes.get(i).getUNIT());
			user.setORG_LEVEL(nodes.get(i).getORG_LEVEL());
			user.setPOLICE_SORT(nodes.get(i).getPOLICE_SORT());
			user.setPOLICE_NO(nodes.get(i).getPOLICE_NO());
			user.setSENSITIVE_LEVEL(nodes.get(i).getSENSITIVE_LEVEL());
			user.setBUSINESS_TYPE(nodes.get(i).getBUSINESS_TYPE());
			user.setTAKE_OFFICE(nodes.get(i).getTAKE_OFFICE());
			user.setUSER_STATUS(nodes.get(i).getUSER_STATUS());
			user.setPosition(nodes.get(i).getPosition());
			user.setDept(nodes.get(i).getDept());
			user.setDELETE_STATUS(User.DELSTATUSYES);
			user.setDATA_VERSION(nodes.get(i).getDATA_VERSION());
			user.setLATEST_MOD_TIME(timenow);
			
			user = dao.UserAdd(user);
		}
		
		return user;
	}
	
	private String generateHash(String idNum) throws Exception {
		
		MessageDigest digest = MessageDigest.getInstance("MD5");

		if(digest == null)
		{
			throw new Exception("generate hash operator fail.");
		}
		byte[] hash = digest.digest(idNum.getBytes());

		String result =  new BASE64Encoder().encode(hash);
		return result;
	}
	
	private String generateSuffix(String idNum) throws Exception {
		
		String value = idNum.substring( idNum.length()-6, idNum.length() );

		return value;
	}
	
	private void AddUserQueryLog(User criteria) throws Exception {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
				Locale.SIMPLIFIED_CHINESE);
		String timenow = sdf.format(new Date());
		
		AuditUserLog auditUserLog = new AuditUserLog();
		AuditLogDAO logdao = new AuditLogDAOImpl();
		AuditLogService als = new AuditLogService();
		
		auditUserLog.setAdminId(als.adminLogin());
		auditUserLog.setIpAddr("");
		auditUserLog.setFlag(AuditUserLog.LOGFLAGQUERY);
		auditUserLog.setResult(AuditUserLog.LOGRESULTSUCCESS);
		auditUserLog.setLATEST_MOD_TIME(timenow);
		auditUserLog = logdao.AuditUserLogAdd(auditUserLog);
		
//		str=criteria.getNAME()+";"+criteria.getBUSINESS_TYPE()+";"
//				+criteria.getPOLICE_SORT()+";"+criteria.getSEXCODE()+";"+criteria.getCERTIFICATE_CODE_SUFFIX()+";"
//				+criteria.getSENSITIVE_LEVEL()+";"+criteria.getPosition()+";"+criteria.getDept()+";"
//				+criteria.getTAKE_OFFICE()+";"+criteria.getPOLICE_NO()
//				;
		if( criteria != null ) {
			AuditUserLogDescribe auditUserLogDescribe = new AuditUserLogDescribe();
			AuditLogDescribeDao logDescdao = new AuditLogDescribeDAOImpl();
			
			auditUserLogDescribe.setLogid(auditUserLog.getId());
			String str="";
			if(criteria.getNAME() != null && criteria.getNAME().length() > 0) {
				str += criteria.getNAME()+";";
			}
			if(criteria.getBUSINESS_TYPE() != null && criteria.getBUSINESS_TYPE().length() > 0) {
				str += criteria.getBUSINESS_TYPE()+";";
			}
			if(criteria.getPOLICE_SORT() != null && criteria.getPOLICE_SORT().length() > 0) {
				str += criteria.getPOLICE_SORT()+";";
			}
			if(criteria.getSEXCODE() != null && criteria.getSEXCODE().length() > 0) {
				str += criteria.getSEXCODE()+";";
			}
			if(criteria.getCERTIFICATE_CODE_SUFFIX() != null && criteria.getCERTIFICATE_CODE_SUFFIX().length() > 0) {
				str += criteria.getCERTIFICATE_CODE_SUFFIX()+";";
			}
			if(criteria.getSENSITIVE_LEVEL() != null && criteria.getSENSITIVE_LEVEL().length() > 0) {
				str += criteria.getSENSITIVE_LEVEL()+";";
			}
			if(criteria.getPosition() != null && criteria.getPosition().length() > 0) {
				str += criteria.getPosition()+";";
			}
			if(criteria.getDept() != null && criteria.getDept().length() > 0) {
				str += criteria.getDept()+";";
			}
			if(criteria.getTAKE_OFFICE() != null && criteria.getTAKE_OFFICE().length() > 0) {
				str += criteria.getTAKE_OFFICE()+";";
			}
			if(criteria.getPOLICE_NO() != null && criteria.getPOLICE_NO().length() > 0) {
				str += criteria.getPOLICE_NO();
			}
			auditUserLogDescribe.setDescrib(str);
			
			auditUserLogDescribe.setLATEST_MOD_TIME(timenow);
			auditUserLogDescribe = logDescdao.AuditUserLogDescribeAdd(auditUserLogDescribe);
		}
	}
	
	private void AddUserAddOrUpdateLog(User user) throws Exception {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
				Locale.SIMPLIFIED_CHINESE);
		String timenow = sdf.format(new Date());
		AuditUserLog auditUserLog = new AuditUserLog();
		AuditLogDAO logdao = new AuditLogDAOImpl();
		AuditLogService als = new AuditLogService();
		
		auditUserLog.setAdminId(als.adminLogin());
		auditUserLog.setIpAddr("");
		if(user.getId() == 0){
			auditUserLog.setFlag(AuditUserLog.LOGFLAGADD);
		}else{
			auditUserLog.setFlag(AuditUserLog.LOGFLAGUPDATE);
		}
		auditUserLog.setResult(AuditUserLog.LOGRESULTSUCCESS);
		auditUserLog.setLATEST_MOD_TIME(timenow);
		auditUserLog = logdao.AuditUserLogAdd(auditUserLog);
		
		AuditUserLogDescribe auditUserLogDescribe = new AuditUserLogDescribe();
		AuditLogDescribeDao logDescdao = new AuditLogDescribeDAOImpl();
		
		auditUserLogDescribe.setLogid(auditUserLog.getId());
		String str="";
//		str=user.getNAME()+";"+user.getCERTIFICATE_CODE_SUFFIX()+";"+user.getSEXCODE()+";"
//				+user.getGA_DEPARTMENT()+";"+user.getUNIT()+";"+user.getPOLICE_SORT()+";"
//				+user.getPOLICE_NO()+";"+user.getSENSITIVE_LEVEL()+";"+user.getBUSINESS_TYPE()+";"
//				+user.getTAKE_OFFICE()+";"+user.getPosition()+";"+user.getDept()
//				;
	
		if(user.getNAME() != null && user.getNAME().length() > 0) {
			str += user.getNAME()+";";
		}
		if(user.getBUSINESS_TYPE() != null && user.getBUSINESS_TYPE().length() > 0) {
			str += user.getBUSINESS_TYPE()+";";
		}
		if(user.getPOLICE_SORT() != null && user.getPOLICE_SORT().length() > 0) {
			str += user.getPOLICE_SORT()+";";
		}
		if(user.getSEXCODE() != null && user.getSEXCODE().length() > 0) {
			str += user.getSEXCODE()+";";
		}
		if(user.getCERTIFICATE_CODE_SUFFIX() != null && user.getCERTIFICATE_CODE_SUFFIX().length() > 0) {
			str += user.getCERTIFICATE_CODE_SUFFIX()+";";
		}
		if(user.getSENSITIVE_LEVEL() != null && user.getSENSITIVE_LEVEL().length() > 0) {
			str += user.getSENSITIVE_LEVEL()+";";
		}
		if(user.getGA_DEPARTMENT() != null && user.getGA_DEPARTMENT().length() > 0) {
			str += user.getGA_DEPARTMENT()+";";
		}
		if(user.getUNIT() != null && user.getUNIT().length() > 0) {
			str += user.getUNIT()+";";
		}
		if(user.getPosition() != null && user.getPosition().length() > 0) {
			str += user.getPosition()+";";
		}
		if(user.getDept() != null && user.getDept().length() > 0) {
			str += user.getDept()+";";
		}
		if(user.getTAKE_OFFICE() != null && user.getTAKE_OFFICE().length() > 0) {
			str += user.getTAKE_OFFICE()+";";
		}
		if(user.getPOLICE_NO() != null && user.getPOLICE_NO().length() > 0) {
			str += user.getPOLICE_NO();
		}
		auditUserLogDescribe.setDescrib(str);
		
		auditUserLogDescribe.setLATEST_MOD_TIME(timenow);
		auditUserLogDescribe = logDescdao.AuditUserLogDescribeAdd(auditUserLogDescribe);
		
		return ;
	}
	
	private void AddUserDelLog(User user) throws Exception {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
				Locale.SIMPLIFIED_CHINESE);
		String timenow = sdf.format(new Date());
		
		AuditUserLog auditUserLog = new AuditUserLog();
		AuditLogDAO logdao = new AuditLogDAOImpl();
		AuditLogService als = new AuditLogService();
		
		auditUserLog.setAdminId(als.adminLogin());
		auditUserLog.setIpAddr("");
		auditUserLog.setFlag(AuditUserLog.LOGFLAGDELETE);
		auditUserLog.setResult(AuditUserLog.LOGRESULTSUCCESS);
		auditUserLog.setLATEST_MOD_TIME(timenow);
		auditUserLog = logdao.AuditUserLogAdd(auditUserLog);
		
		AuditUserLogDescribe auditUserLogDescribe = new AuditUserLogDescribe();
		AuditLogDescribeDao logDescdao = new AuditLogDescribeDAOImpl();
		
		auditUserLogDescribe.setLogid(auditUserLog.getId());
		
		UserDAO dao = new UserDAOImpl();
		List<User> nodes = dao.GetUserById(user.getId());
		String str="";
		for (int i = 0; i < nodes.size(); i++) {
			if(nodes.get(i).getNAME() != null && nodes.get(i).getNAME().length() > 0) {
				str += nodes.get(i).getNAME()+";";
			}
			if(nodes.get(i).getBUSINESS_TYPE() != null && nodes.get(i).getBUSINESS_TYPE().length() > 0) {
				str += nodes.get(i).getBUSINESS_TYPE()+";";
			}
			if(nodes.get(i).getPOLICE_SORT() != null && nodes.get(i).getPOLICE_SORT().length() > 0) {
				str += nodes.get(i).getPOLICE_SORT()+";";
			}
			if(nodes.get(i).getSEXCODE() != null && nodes.get(i).getSEXCODE().length() > 0) {
				str += nodes.get(i).getSEXCODE()+";";
			}
			if(nodes.get(i).getCERTIFICATE_CODE_SUFFIX() != null && nodes.get(i).getCERTIFICATE_CODE_SUFFIX().length() > 0) {
				str += nodes.get(i).getCERTIFICATE_CODE_SUFFIX()+";";
			}
			if(nodes.get(i).getSENSITIVE_LEVEL() != null && nodes.get(i).getSENSITIVE_LEVEL().length() > 0) {
				str += nodes.get(i).getSENSITIVE_LEVEL()+";";
			}
			if(nodes.get(i).getGA_DEPARTMENT() != null && nodes.get(i).getGA_DEPARTMENT().length() > 0) {
				str += nodes.get(i).getGA_DEPARTMENT()+";";
			}
			if(nodes.get(i).getUNIT() != null && nodes.get(i).getUNIT().length() > 0) {
				str += nodes.get(i).getUNIT()+";";
			}
			if(nodes.get(i).getPosition() != null && nodes.get(i).getPosition().length() > 0) {
				str += nodes.get(i).getPosition()+";";
			}
			if(nodes.get(i).getDept() != null && nodes.get(i).getDept().length() > 0) {
				str += nodes.get(i).getDept()+";";
			}
			if(nodes.get(i).getTAKE_OFFICE() != null && nodes.get(i).getTAKE_OFFICE().length() > 0) {
				str += nodes.get(i).getTAKE_OFFICE()+";";
			}
			if(nodes.get(i).getPOLICE_NO() != null && nodes.get(i).getPOLICE_NO().length() > 0) {
				str += nodes.get(i).getPOLICE_NO();
			}
		}
		
		auditUserLogDescribe.setDescrib(str);
		
		auditUserLogDescribe.setLATEST_MOD_TIME(timenow);
		auditUserLogDescribe = logDescdao.AuditUserLogDescribeAdd(auditUserLogDescribe);
	}
}
