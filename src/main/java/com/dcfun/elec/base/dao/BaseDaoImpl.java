package com.dcfun.elec.base.dao;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Resource;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import com.dcfun.elec.utils.TUtils;

public class BaseDaoImpl<T> extends HibernateDaoSupport implements IBaseDao<T> {

	Class entityClass = TUtils.getTClass(this.getClass());

	@Resource(name = "sessionFactory")
	public final void setSessionFactoryDi(SessionFactory sessionFactory) {
		this.setSessionFactory(sessionFactory);
	}

	public void save(T entity) {
		this.getHibernateTemplate().save(entity);
	}

	// 更新
	public void update(T entity) {
		this.getHibernateTemplate().update(entity);
	}

	// 根据Id查
	public T findObjectById(Serializable id) {
		return (T) this.getHibernateTemplate().get(entityClass, id);
	}

	// 使用主键ID 删除一个或多个对象
	public void deleteObjectByIDs(Serializable... ids) {
		if (ids != null && ids.length > 0) {
			for (Serializable id : ids) {
				Object entity = this.findObjectById(id);
				if (entity != null) {
					this.getHibernateTemplate().delete(entity);
				}
			}
		}

	}

	@Override
	public void deleteObjectByCollection(List<T> list) {
		this.getHibernateTemplate().deleteAll(list);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<T> findCollectionByConditionNoPage(
			final Map<String, Object> keyValues, Map<String, String> orderby) {

		final StringBuffer finalHql = new StringBuffer();
		finalHql.append("from " + entityClass.getSimpleName()+" o");
		finalHql.append(" where 1=1");
		finalHql.append(this.buildWhere(keyValues));
		finalHql.append(this.buildOrderBy(orderby));
		List<T> list = this.getHibernateTemplate().execute(
				new HibernateCallback() {

					public Object doInHibernate(Session session)
							throws HibernateException, SQLException {

						Query query = session.createQuery(finalHql.toString());
						if (keyValues != null) {
							for (Entry<String, Object> entry : keyValues.entrySet()) {
								
								
								/**2016-04-25 18:36:11 灵机一动
								 * 
								 * 		entry.getValue() 中加个 "("...")"不就可以定位到了么。。。哎、蠢！
								 * */
//								if(entry.getKey().contains("in")){
//									/**2016-04-24 14:02:04 添加 --- 封装带in的查询条件*/
//									if (entry.getKey().contains(".")) {
//										query.setParameter(entry.getKey().split("\\.")[1].substring(0, entry.getKey().length()-4), entry.getValue());
//									}else {
//										query.setParameter(entry.getKey().substring(0, entry.getKey().length()-3), entry.getValue());
//									}
//								}else
								if (entry.getKey().contains("like")) {
									
									if(entry.getKey().contains(".")){
										
										query.setParameter(entry.getKey().split("\\.")[1].substring(0, entry.getKey().length()-5), entry.getValue());
									}else{
										//最简单的情况，没有"."的  “>=”、“<=” 
										//为了支持 >=、<=  也是拼 [笑Cry]
 										query.setParameter(entry.getKey().substring(0, entry.getKey().length()-5), "'"+entry.getValue());
									}
									
								}else if(entry.getKey().contains("=")){
									
									if(entry.getKey().contains(".")){
										query.setParameter(entry.getKey().split("\\.")[1].split("\\=")[0].substring(0, entry.getKey().length()-3), entry.getValue());
									}else{
										//最简单的情况，没有"."的  “>=”、“<=” 
										//为了支持 >=、<=  也是拼 [笑Cry]
										query.setParameter(entry.getKey().split("\\=")[0].substring(0, entry.getKey().length()-3), entry.getValue());
									}
									
								}else if(entry.getKey().contains(".")){
									query.setParameter(entry.getKey().split("\\.")[1], entry.getValue());
								}else{
									query.setParameter(entry.getKey(), entry.getValue());
								}
								
							}
						}

						return query.list();
					}

				});
		return list;
	}

	private String buildOrderBy(Map<String, String> orderby) {
		StringBuffer buffer = new StringBuffer("");
		if(orderby!=null && orderby.size()>0){
			buffer.append(" ORDER BY ");
			for(Map.Entry<String, String> map:orderby.entrySet()){
				buffer.append(map.getKey()+" "+map.getValue()+",");
			}
			//在循环后，删除最后一个逗号
			buffer.deleteCharAt(buffer.length()-1);
		}
		return buffer.toString();
	}

	private StringBuffer buildWhere(Map<String, Object> keyValues) {
		final StringBuffer finalHql = new StringBuffer();
		if (keyValues != null) {
			
			for (Entry<String, Object> entry : keyValues.entrySet()) {// 把查询条件放到where的后面
				
//				if(entry.getKey().contains("in")){
//					/**2016-04-24 14:02:04 添加 --- 封装带in的查询条件*/hibernate 找不到in(:mid) mid!!! 
//					if (entry.getKey().contains(".")) {				//entry.getKey().split("\\.")[1].substring(0, entry.getKey().length()-4)
//						System.out.println("finalHql.append:"+entry.getKey().split("\\.")[1].substring(0, entry.getKey().split("\\.")[1].length()-3));
//						finalHql.append(" and "+entry.getKey()+" :"+entry.getKey().split("\\.")[1].substring(0, entry.getKey().split("\\.")[1].length()-3));
//						System.out.println("finalHql = "+finalHql);
//					}else {
//						//先写不带"." 的，  Mid in ( :Mid)
//						finalHql.append(" and "+entry.getKey()+"(:"+entry.getKey().substring(0, entry.getKey().length()-3)+" )");
//					}
//				}else 
					if (entry.getKey().contains("like")) {
					if(entry.getKey().contains(".")){
						finalHql.append(" and "+entry.getKey()+":"+entry.getKey().split("\\.")[1].substring(0, entry.getKey().length()-5));//用了两个split 只为把“>”、“<”分出来
					}else{
						//最简单的情况，没有"."的  “like” 
						
						finalHql.append(" and "+entry.getKey()+":"+entry.getKey().substring(0, entry.getKey().length()-5));
					}
					
				}else if(entry.getKey().contains("=")){
					
					if(entry.getKey().contains(".")){
						finalHql.append(" and "+entry.getKey().split("\\.")[1].split("\\=")[0]+"= :"+entry.getKey().split("\\.")[1].split("\\=")[0].substring(0, entry.getKey().length()-4));//用了两个split 只为把“>”、“<”分出来
					}else{
						//最简单的情况，没有"."的  “>=”、“<=” 
						finalHql.append(" and "+entry.getKey().split("\\=")[0]+"= :"+entry.getKey().split("\\=")[0].substring(0, entry.getKey().length()-4));
					}
					
					
				}else if(entry.getKey().contains(".")){
					finalHql.append(" and "+entry.getKey()+"=:"+entry.getKey().split("\\.")[1]);
				}else{
					finalHql.append(" and "+entry.getKey()+"=:"+entry.getKey());
				}
			}
		}
		return finalHql;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<T> findCollectionByConditionNoPageWithCache(
			final Map<String, Object> keyValues, Map<String, String> orderby) {

		final StringBuffer finalHql = new StringBuffer();
		finalHql.append("from " + entityClass.getSimpleName()+" o");
		finalHql.append(" where 1=1");
		finalHql.append(this.buildWhere(keyValues));
		finalHql.append(this.buildOrderBy(orderby));
		List<T> list = this.getHibernateTemplate().execute(
				new HibernateCallback() {

					public Object doInHibernate(Session session)
							throws HibernateException, SQLException {

						Query query = session.createQuery(finalHql.toString());
						if (keyValues != null) {
							for (Entry<String, Object> entry : keyValues.entrySet()) {
								
//								if(entry.getKey().contains("in")){
//									/**2016-04-24 14:02:04 添加 --- 封装带in的查询条件*/
//									if (entry.getKey().contains(".")) {
//										System.out.println("setParameter:"+entry.getKey().split("\\.")[1].substring(0, entry.getKey().split("\\.")[1].length()-3)+"++"+entry.getValue());
//										query.setParameter(entry.getKey().split("\\.")[1].substring(0, entry.getKey().length()-4), entry.getValue());
//									}else {
//										query.setParameter(entry.getKey().substring(0, entry.getKey().length()-3), entry.getValue());
//									}
//								}else
								if (entry.getKey().contains("like")) {
									
									if(entry.getKey().contains(".")){
										
										query.setParameter(entry.getKey().split("\\.")[1].substring(0, entry.getKey().length()-5), entry.getValue());
									}else{
										//最简单的情况，没有"."的  “>=”、“<=” 
										//为了支持 >=、<=  也是拼 [笑Cry]
 										query.setParameter(entry.getKey().substring(0, entry.getKey().length()-5), "'"+entry.getValue());
									}
									
								}else if(entry.getKey().contains("=")){
									
									if(entry.getKey().contains(".")){
										query.setParameter(entry.getKey().split("\\.")[1].split("\\=")[0].substring(0, entry.getKey().length()-3), entry.getValue());
									}else{
										//最简单的情况，没有"."的  “>=”、“<=” 
										//为了支持 >=、<=  也是拼 [笑Cry]
										query.setParameter(entry.getKey().split("\\=")[0].substring(0, entry.getKey().length()-3), entry.getValue());
									}
									
								}else if(entry.getKey().contains(".")){
									query.setParameter(entry.getKey().split("\\.")[1], entry.getValue());
								}else{
									query.setParameter(entry.getKey(), entry.getValue());
								}
								
							}
						}
						query.setCacheable(true);
						
						return query.list();
					}

				});
		return list;
	}

}
