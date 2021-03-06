package com.jk.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.jk.dao.CarDao;
import com.jk.dao.CarDaos;
import com.jk.pojo.CarBean;
import com.jk.dao.EmpDao;
import com.jk.dao.EsDao;
import com.jk.pojo.ClassBean;
import com.jk.pojo.EmpBean;
import com.jk.pojo.StuBean;
import com.jk.dao.bookDao;
import com.jk.pojo.TreeBean;
import com.jk.pojo.ntfclass;
import com.jk.pojo.ntfjob;
import com.jk.pojo.bookBean;
import com.jk.service.CarService;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.*;

import org.springframework.util.StringUtils;

import java.util.*;

/**
 * @program: es-mysql
 * @description:
 * @author: 刘洋朋
 * @create: 2021-01-05 19:22
 */
@Service
public class CarServiceImpl implements CarService {

    @Autowired
    private CarDao carDao;
    @Autowired
    private bookDao bookdao;



    @Autowired
    private ElasticsearchTemplate esTemlpate;
    @Autowired
    private CarDaos carDaos;

    @Autowired
    private EmpDao empDao;

    @Autowired
    private EsDao esDao;

    @Autowired
    private ElasticsearchTemplate esTemplate;

    @Autowired
    private  ElasticsearchTemplate elasticsearchTemplate;
    @Override
    public List<TreeBean> findTree() {
        int pid=0;
        List<TreeBean> list = findNodes(pid);
        return list;
    }

    @Override
    public HashMap<String, Object> efindTable(Integer page, Integer rows, EmpBean empBean) {
        HashMap<String, Object> map = new HashMap<>();
        Client client = esTemplate.getClient();
        SearchRequestBuilder searchRequestBuilder = client.prepareSearch("emp")//设置查询索引库 数据库
                .setTypes("2006a");////设置查询类型 表
        BoolQueryBuilder bool = new BoolQueryBuilder();
        if (!StringUtils.isEmpty(empBean.getYewu())){
            bool.should(QueryBuilders.matchQuery("name",empBean.getYewu()));
            bool.should(QueryBuilders.matchQuery("zname",empBean.getYewu()));
        }
        searchRequestBuilder.setQuery(bool);

        searchRequestBuilder.addSort("time", SortOrder.DESC);


        int start=(page-1)*rows;
        searchRequestBuilder.setFrom(start);
        searchRequestBuilder.setSize(rows);


        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("name");
        highlightBuilder.field("zname");
        highlightBuilder.preTags("<font color=\"red\">");//前缀
        highlightBuilder.postTags("</font>");//后缀
        searchRequestBuilder.highlighter(highlightBuilder);


        SearchResponse searchResponse = searchRequestBuilder.get();

        SearchHits hits = searchResponse.getHits();

        long total = hits.getTotalHits();

        Iterator<SearchHit> iterator = hits.iterator();
        List<EmpBean> list = new ArrayList<>();
        while (iterator.hasNext()){
            SearchHit next = iterator.next();
            String str = next.getSourceAsString();

            EmpBean empBean1 = JSONObject.parseObject(str, EmpBean.class);

            Map<String, HighlightField> highlightFields = next.getHighlightFields();
            HighlightField name = highlightFields.get("name");
            if (name!=null){
                String name2 = name.getFragments()[0].toString();
                empBean1.setName(name2);
            }
            HighlightField zname = highlightFields.get("zname");
            if (zname!=null){
                String zname2 = zname.getFragments()[0].toString();
                empBean1.setZname(zname2);
            }
            list.add(empBean1);
        }
        map.put("rows",list);
        map.put("total",total);
        return map;
    }

    @Override
    public void addEmp(EmpBean empBean) {
        if (empBean.getId()==null) {
            carDao.addEmp(empBean);
        }else {
            carDao.updateEmp(empBean);
        }
        empDao.save(empBean);

    }

    @Override
    public Optional<EmpBean> findEmp(Integer id) {
        Optional<EmpBean> empBean = empDao.findById(id);
        return empBean;
    }

    @Override
    public void delEmp(Integer id) {
        empDao.deleteById(id);
        carDao.delEmp(id);

    }

    @Override
    public HashMap<String, Object> findcarList(Integer page, Integer rows, bookBean book) {
        List<bookBean> list = new ArrayList<>();
        HashMap<String, Object> map = new HashMap<String, Object>();
        //1、获取es客户端对象
        Client client = esTemlpate.getClient();
        //2、创建查询对象：设置索引、类型
        SearchRequestBuilder search = client.prepareSearch("book")//索引、数据库
                .setTypes("book");//类型、表
        //分页
        search.setFrom((page-1)*rows);//开始位置
        search.setSize(rows);//没有条数
        //3、执行、获取查询结果
        SearchResponse searchResponse = search.get();
        SearchHits hits = searchResponse.getHits();
        Iterator<SearchHit> iterator = hits.iterator();
        while (iterator.hasNext()){
            SearchHit next = iterator.next();
            String str = next.getSourceAsString();
            //把字符串转换成javabean对象
            bookBean jobBean = JSONObject.parseObject(str, bookBean.class);
            list.add(jobBean);
        }
        //获取总条数：
        long total = hits.getTotalHits();
        map.put("total",total);
        map.put("rows",list);
        return map;
    }

    @Override
    public void toadd(bookBean book) {
     int random=(int) Math.floor(Math.random()*8999+1000);
        if(book.getId()!=null){
            carDao.updateid(book);
        }else{
            book.setId(random);
            carDao.toadd(book);
        }
        book.setId(random);
        bookdao.save(book);

    }

    @Override
    public void delJobById(Integer id) {
        bookdao.deleteById(id);
        carDao.delJobByIds(id);

    }

    @Override
    public Optional<bookBean> findJobById(Integer id) {
        return bookdao.findById(id);
    }

    private List<TreeBean> findNodes(int pid) {
        List<TreeBean> list = carDao.findTreeByPid(pid);
        for (TreeBean tree : list) {
            Integer id = tree.getId();
            List<TreeBean> nodelist = findNodes(id);//递归自己调自己
            //判断是否有子节点：有子节点-->打开  false  没有子节点-->不能打开 true
            if(nodelist!=null&&nodelist.size()>0){//有子节点
                tree.setNodes(nodelist);
                tree.setSelectable(false);//有子节点-->打开  false
            }else{//没有子节点
                tree.setSelectable(true);//没有子节点-->不能打开 true
            }
        }
        return list;
    }

    @Override
    public HashMap<String, Object> ntffindJob(Integer page, Integer rows, ntfjob job) {
        int count = carDao.selectCount(job);
        int start =(page-1)*rows+1;
        int end =page*rows;

        List list = carDao.select(start,end,job);
        HashMap<String, Object>map= new HashMap<>();
        map.put("total", count);
        map.put("rows", list);

        return map;
    }

    @Override
    public void ntfaddJob(ntfjob job) {
        Integer classId = job.getClassId();
        ntfclass classBean=carDao.ntffindClassName(classId);
        job.setClassName(classBean.getClassName());
        if(job.getId()==null){
            carDao.ntfaddJob(job);
        }else{
            carDao.ntfupdJob(job);
        }
    }

    @Override
    public Optional<ntfjob> ntffindJobById(Integer id) {
        return carDao.ntffindById(id);
    }

    @Override
    public void ntfdelJobById(Integer id) {
        carDao.ntfdeleteById(id);
        carDao.ntfdelJobByIds(id);
    }

    @Override
    public List<ntfclass> ntffindClass() {
        return carDao.ntffindClass();
    }

    @Override
    public void delJob(Integer id) {
        carDao.delJob(id);
    }


    @Override
    public HashMap<String, Object> findCar(Integer page, Integer rows, CarBean car) {
        List<CarBean> list = new ArrayList<>();
        HashMap<String, Object> map = new HashMap<String, Object>();
        //1、获取es客户端对象
        Client client = esTemlpate.getClient();
        //2、创建查询对象：设置索引、类型
        SearchRequestBuilder search = client.prepareSearch("lypcar")//索引、数据库
                .setTypes("car");//类型、表
/*
        //条查
        if(!StringUtils.isEmpty(book.getName())){
            search.setQuery(QueryBuilders.matchQuery("name",book.getName()));
        }
*/
        //混合查询、组合查询：同时查询名称、简介
        BoolQueryBuilder bool = new BoolQueryBuilder();
        if(!StringUtils.isEmpty(car.getCarName())){
            //查询名称
            bool.must(QueryBuilders.matchQuery("carName",car.getCarName()));
            //查询简介
        }
        RangeQueryBuilder uploadTime = QueryBuilders.rangeQuery("carPrice");
        if(car.getMinPay()!=null){
            uploadTime.gt(car.getMinPay());
        }

        if(car.getMaxPay()!=null){
            uploadTime.lt(car.getMaxPay());
        }
        if(car.getMinPay()!=null || car.getMaxPay()!=null){
            bool.must(uploadTime);
        }
        search.setQuery(bool);
        //设置高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("carName");
        // <font color="red"></font>
        highlightBuilder.preTags("<font color=\"red\">");//前缀
        highlightBuilder.postTags("</font>");//后缀
        search.highlighter(highlightBuilder);
        search.addSort("carPrice", SortOrder.ASC);
        //分页
        search.setFrom((page-1)*rows);//开始位置
        search.setSize(rows);//没有条数
        //3、执行、获取查询结果
        SearchResponse searchResponse = search.get();
        SearchHits hits = searchResponse.getHits();
        Iterator<SearchHit> iterator = hits.iterator();
        while (iterator.hasNext()){
            SearchHit next = iterator.next();
            String str = next.getSourceAsString();
            //把字符串转换成javabean对象
            CarBean jobBean = JSONObject.parseObject(str, CarBean.class);
            //获取name的高亮内容
            Map<String, HighlightField> highlightFields = next.getHighlightFields();
            HighlightField name = highlightFields.get("carName");
            if(name!=null){
                /*Text[] fragments = name.getFragments();
                Text fragment = fragments[0];
                String s = fragment.toString();*/
                String name2 = name.getFragments()[0].toString();
                jobBean.setCarName(name2);
            }
            list.add(jobBean);
        }
        //获取总条数：
        long total = hits.getTotalHits();
        map.put("total",total);
        map.put("rows",list);
        return map;
    }

    @Override
    public void addCar(CarBean car) {
        if(car.getId()==null){
            carDao.addCar(car);
        }else{
            carDao.updCar(car);
        }
        carDaos.save(car);
    }

    @Override
    public Optional<CarBean> findCarById(Integer id) {
        return carDaos.findById(id);
    }

    @Override
    public void delCarById(Integer id) {
        carDaos.deleteById(id);
        carDao.delById(id);
    }



    @Override
    public HashMap<String, Object> initTable(Integer page, Integer rows, StuBean stuBean) {
        HashMap<String, Object> map = new HashMap<String, Object>();

        List<StuBean> list = new ArrayList<>();

        Client client = esTemplate.getClient();
        SearchRequestBuilder search = client.prepareSearch("stu")//索引、数据库
                .setTypes("2006a");//类型、表
        BoolQueryBuilder bool = new BoolQueryBuilder();
        if(!StringUtils.isEmpty(stuBean.getYewu())){
            bool.must(QueryBuilders.matchQuery("name",stuBean.getYewu()));
            bool.must(QueryBuilders.matchQuery("util",stuBean.getYewu()));
        }
        RangeQueryBuilder price = QueryBuilders.rangeQuery("salary");
        if(stuBean.getMinsalary()!=null){
            price.gte(stuBean.getMinsalary());
        }

        if(stuBean.getMaxsalary()!=null){
            price.lte(stuBean.getMaxsalary());
        }
        if(stuBean.getMinsalary()!=null || stuBean.getMaxsalary()!=null){
            bool.must(price);
        }
        search.setQuery(bool);


        //设置高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("name");//名称高亮
        highlightBuilder.field("util");//简介高亮
        // <font color="red"></font>
        highlightBuilder.preTags("<font color=\"red\">");//前缀
        highlightBuilder.postTags("</font>");//后缀
        search.highlighter(highlightBuilder);

        //排序: 先价格升序、id降序
        search.addSort("age", SortOrder.ASC);
        search.addSort("salary", SortOrder.DESC);

        //分页
        search.setFrom((page-1)*rows);//开始位置
        search.setSize(rows);//没有条数

        //3、执行、获取查询结果
        SearchResponse searchResponse = search.get();

        SearchHits hits = searchResponse.getHits();

        Iterator<SearchHit> iterator = hits.iterator();
        while (iterator.hasNext()){
            SearchHit next = iterator.next();
            String str = next.getSourceAsString();
            //把字符串转换成javabean对象
            StuBean stuBean1 = JSONObject.parseObject(str, StuBean.class);

            //获取name的高亮内容
            Map<String, HighlightField> highlightFields = next.getHighlightFields();
            HighlightField name = highlightFields.get("name");
            if(name!=null){

                String name2 = name.getFragments()[0].toString();
                stuBean1.setName(name2);
            }

            //处理简介高亮
            HighlightField util = highlightFields.get("util");
            if(util!=null){
                String info2 = util.getFragments()[0].toString();
                stuBean1.setUtil(info2);
            }

            list.add(stuBean1);
        }

        //获取总条数：
        long total = hits.getTotalHits();
        map.put("total",total);
        map.put("rows",list);
        return map;
    }


    @Override
    public List<ClassBean> findclass() {
        return carDao.findclass();
    }

    @Override
    public StuBean findStu(Integer id) {
        Optional<StuBean> stuBean=esDao.findById(id);
        StuBean stuBean1 = stuBean.get();
        return stuBean1;
    }

    @Override
    public void addStu(StuBean stuBean) {
        ClassBean classBean=carDao.findClassById(stuBean.getClassid());
        stuBean.setClassname(classBean.getClassname());
        if (stuBean.getId()==null) {
            carDao.addStu(stuBean);
        }else {
            carDao.updateStu(stuBean);
        }
        esDao.save(stuBean);
    }

    @Override
    public void delStu(Integer id) {
        esDao.deleteById(id);
        carDao.delStu(id);
    }

}
