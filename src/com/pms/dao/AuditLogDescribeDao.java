package com.pms.dao;


import java.util.List;

import com.pms.model.AuditGroupLogDescribe;
import com.pms.model.AuditOrgLogDescribe;
import com.pms.model.AuditPrivLogDescribe;
import com.pms.model.AuditResLogDescribe;
import com.pms.model.AuditUserLogDescribe;

public interface AuditLogDescribeDao {
	AuditUserLogDescribe AuditUserLogDescribeAdd(AuditUserLogDescribe auditUserLogDescribe) throws Exception;
	List<AuditUserLogDescribe> GetUserLogDescByLogId(int logid) throws Exception;
	AuditOrgLogDescribe AuditOrgLogDescribeAdd(AuditOrgLogDescribe auditOrgLogDescribe) throws Exception;
	List<AuditOrgLogDescribe> GetOrgLogDescByLogId(int logid) throws Exception;
	AuditGroupLogDescribe AuditGroupLogDescribeAdd(AuditGroupLogDescribe auditGroupLogDescribe) throws Exception;
	List<AuditGroupLogDescribe> GetGroupLogDescByLogId(int logid) throws Exception;
	AuditResLogDescribe AuditResLogDescribeAdd(AuditResLogDescribe auditResLogDescribe) throws Exception;
	List<AuditResLogDescribe> GetResLogDescByLogId(int logid) throws Exception;
	AuditPrivLogDescribe AuditPrivLogDescribeAdd(AuditPrivLogDescribe auditPrivLogDescribe) throws Exception;
	List<AuditPrivLogDescribe> GetPrivLogDescByLogId(int logid) throws Exception;
}