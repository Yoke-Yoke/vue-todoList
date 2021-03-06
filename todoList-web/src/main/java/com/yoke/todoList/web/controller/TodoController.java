package com.yoke.todoList.web.controller;

import com.yoke.todoList.pojo.Record;
import com.yoke.todoList.pojo.RecordExample;
import com.yoke.todoList.pojo.Todo;
import com.yoke.todoList.pojo.TodoExample;
import com.yoke.todoList.service.RecordService;
import com.yoke.todoList.service.TodoService;
import com.yoke.todoList.web.common.TodoListResult;
import com.yoke.todoList.web.dto.TodoDto;
import com.yoke.todoList.web.util.ResultUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Yoke
 * Created on 2018/4/6
 */
@RestController
@RequestMapping("/todo")
public class TodoController {
    private static final Logger LOGGER = LoggerFactory.getLogger(TodoController.class);
    @Autowired
    private TodoService todoService;
    @Autowired
    private RecordService recordService;

    @GetMapping("/list")
    public TodoListResult<Object> list() {
        TodoExample todoExample = new TodoExample();
        todoExample.or().andIsDeleteEqualTo(false);
        RecordExample recordExample = new RecordExample();
        List<TodoDto> todoDtoList = todoService.selectByExample(todoExample).stream()
                .map(todo -> {
                    TodoDto todoDto = new TodoDto();
                    recordExample.or().andTodoIdEqualTo(todo.getId());
                    todoDto.setTodo(todo);
                    todoDto.setRecords(recordService.selectByExampleWithBLOBs(recordExample));
                    recordExample.clear();
                    return todoDto;
                }).collect(Collectors.toList());
        return new ResultUtil<>().setData(todoDtoList);
    }

    @PostMapping("/add")
    public TodoListResult<Object> add() {
        Todo todo = new Todo("new Line", 0, false, false);
        TodoDto todoDto = new TodoDto();
        todoDto.setTodo(null);
        todoService.insert(todo);
        return new ResultUtil<>().setData(null);
    }

    @GetMapping("/detail/{todoId}")
    public TodoListResult<Object> details(@PathVariable Integer todoId) {
        Todo todo = todoService.selectByPrimaryKey(todoId);
        RecordExample recordExample = new RecordExample();
        recordExample.or().andTodoIdEqualTo(todoId);
        List<Record> recordList = recordService.selectByExampleWithBLOBs(recordExample);
        return new ResultUtil<>().setData(new TodoDto(todo, recordList));
    }


    @PostMapping("/addRecord/{id}")
    public TodoListResult<Object> addRecord(@PathVariable Integer id, @RequestBody String text) {
        if (StringUtils.isEmpty(text)) {
            return new ResultUtil<>().setError("no text");
        }
        Todo todo = todoService.selectByPrimaryKey(id);
        Record record = new Record();
        record.setTodoId(id);
        record.setChecked((false));
        record.setIsDelete(false);
        record.setText(text);
        todo.setCount(todo.getCount() + 1);
        todoService.updateByPrimaryKeySelective(todo);
        recordService.insert(record);
        return new ResultUtil<>().setData("success");
    }

    @PostMapping("/editTodo")
    public TodoListResult<Object> updateTodo(@RequestBody Todo todo) {
        LOGGER.info(todo.toString());
        Todo oldTodo = null;
        try {
            oldTodo = todoService.selectByPrimaryKey(todo.getId());
        } catch (NullPointerException e) {
            LOGGER.error("更新的todo没有id信息{}", e.getMessage());
        }
        if (oldTodo == null) {
            LOGGER.info("没有查询得到该todo的信息");
            return new ResultUtil<>().setData(null);
        }
        int count = todoService.updateByPrimaryKeySelective(todo);
        return new ResultUtil<>().setData(null);
    }

    @PostMapping("/editRecord")
    public TodoListResult<Object> updateRecord(@RequestBody Record record) {
        LOGGER.info(record.toString());
        Record oldRecord = recordService.selectByPrimaryKey(record.getId());
        if (oldRecord == null) {
            LOGGER.info("没有查询得到该record的信息");
            return new ResultUtil<>().setData(null);
        }
        if (record.getIsDelete()) {
            Todo todo = todoService.selectByPrimaryKey(record.getTodoId());
            todo.setCount(todo.getCount() - 1);
            todoService.updateByPrimaryKey(todo);
        }
        recordService.updateByPrimaryKeyWithBLOBs(record);
        return new ResultUtil<>().setData(null);
    }
}
