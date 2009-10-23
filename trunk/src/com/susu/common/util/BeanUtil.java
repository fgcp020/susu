package com.susu.common.util;

public class BeanUtil {
	/**
	 * 获取一个对象里面某个属性的值，该属性必须是public 类型
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
