package com.susu.common.util;

public class BeanUtil {
	/**
	 * ��ȡһ����������ĳ�����Ե�ֵ�������Ա�����public ����
	 * @param fieldName
	 * @param obj
	 * @return
	 */
	public static Object getValueByField(String fieldName,Object obj){
		Object ret=null;
		try {
			ret=obj.getClass().getField(fieldName).get(obj);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}
}
