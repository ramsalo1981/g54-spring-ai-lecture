package se.lexicon.g54springai.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class AppToolCalling {

    List<String> storage = new ArrayList<>();

    public AppToolCalling() {
        storage.addAll(Arrays.asList("Mehrdad", "Elnaz", "Simon", "Marcus"));
    }

    @Tool(description = "Fetches all names from the application.")
    public List<String> fetchAllNames() {
        return storage.stream().toList();
    }

    @Tool(description = "Adds a new name to the application.")
    public String addNewName(String name) {
        storage.add(name);
        return "Operation successful. New name added: " + name;
    }

    @Tool(description = "Finds names containing the specified substring.")
    public String findNameByName(String name) {
        System.out.println("name = " + name);
        List<String> foundNames = storage.stream().filter(n -> n.toLowerCase().contains(name.toLowerCase())).toList();
        if (foundNames.isEmpty()){
            return "No names found containing: " + name;
        } else {
            return "Found names: " + String.join(", ", foundNames);
        }
    }
}