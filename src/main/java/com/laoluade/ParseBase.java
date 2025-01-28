package com.laoluade;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.util.Scanner;
import java.util.Iterator;
import org.json.JSONObject;


public class ParseBase {
    public static void main(String[] args) throws IOException {
        // Read input from a .txt file
        String file_path = "Casual Roleplay Base.txt";
        String[] file_lines = new String[0];

        // Try to retrieve the text from the file
        try {
            File myObj = new File(file_path);
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                file_lines = append_line(file_lines, data);
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred. The file was not found.");
        }

        // Parse and convert to JSON
        JSONObject parsed_json = parse_to_json(file_lines);

        // Retrieve a list of characters, locations, and songs
        String[] characters = get_items(parsed_json, 1);
        String[] locations = get_items(parsed_json, 2);
        String[] songs = get_items(parsed_json, 3);

        // Save the parsed JSON information to a folder
        String output_dir = "Output";
        String output_name = "Casual_Roleplay";
        String output_path = output_dir + "/" + output_name;

        File dir_obj = new File(output_dir);
        boolean dir_created;
        if (!dir_obj.exists()) {
            dir_created = dir_obj.mkdir();
        }
        else {
            dir_created = true;
        }

        if (dir_created) {
            // JSON
            FileWriter writer = new FileWriter(output_path + ".json");
            writer.write(parsed_json.toString(4));
            writer.close();
            System.out.println("Parsed JSON has been saved to " + output_path + ".json.");

            // Characters
            BufferedWriter bwriter = new BufferedWriter(new FileWriter(output_path + "_characters.txt."));
            for (String item: characters) {
                bwriter.write(item);
                bwriter.newLine();
            }
            bwriter.close();
            System.out.println("Characters have been saved to " + output_path + "_characters.txt.");

            // Locations
            bwriter = new BufferedWriter(new FileWriter(output_path + "_locations.txt."));
            for (String item: locations) {
                bwriter.write(item);
                bwriter.newLine();
            }
            bwriter.close();
            System.out.println("Locations have been saved to " + output_path + "_locations.txt.");

            // Songs
            bwriter = new BufferedWriter(new FileWriter(output_path + "_songs.txt."));
            for (String item: songs) {
                bwriter.write(item);
                bwriter.newLine();
            }
            bwriter.close();
            System.out.println("Songs have been saved to " + output_path + "_songs.txt.");
        }
    }

    public static String[] append_line(String[] file_lines, String line) {
        String[] new_file_lines = new String[file_lines.length + 1];

        System.arraycopy(file_lines, 0, new_file_lines, 0, file_lines.length);
        new_file_lines[file_lines.length] = line;

        return new_file_lines;
    }

    public static JSONObject parse_to_json(String[] file_lines) {
        JSONObject info = new JSONObject();

        String last_season = null;
        String last_episode = null;
        String last_attribute = null;
        String last_content = null;
        String last_specific = null;

        for (String line: file_lines) {
            boolean starts_with_star = line.startsWith("*");
            boolean starts_with_space = line.startsWith(" ");
            int amount_leading_space = line.length() - line.stripLeading().length();

            if (!(starts_with_star || starts_with_space)) {  // Season
                line = line.replaceFirst("^\uFEFF", "");
                info.put(line, new JSONObject());
                last_season = line;
            }
            else if (starts_with_star) {  // Episode
                line = line.replace('*', ' ').strip();
                JSONObject cur_season = info.getJSONObject(last_season);
                cur_season.put(line, new JSONObject());
                last_episode = line;
            }
            else if (starts_with_space && amount_leading_space == 3) {  // Attributes
                line = line.replace('*', ' ').strip();
                JSONObject cur_season = info.getJSONObject(last_season);
                JSONObject cur_episode = cur_season.getJSONObject(last_episode);

                if (line.equals("Songs")) {
                    cur_episode.put(line, new JSONObject());
                }
                else {
                    cur_episode.put(line, new String[0]);
                }

                last_attribute = line;
            }
            else if (starts_with_space && amount_leading_space == 6) {  // Content
                line = line.replace('*', ' ').strip();
                JSONObject cur_season = info.getJSONObject(last_season);
                JSONObject cur_episode = cur_season.getJSONObject(last_episode);

                assert last_attribute != null;

                if (last_attribute.equals("Songs")) {
                    JSONObject cur_attribute = cur_episode.getJSONObject(last_attribute);
                    cur_attribute.put(line, new JSONObject());
                }
                else {
                    String[] cur_attr_array = (String[]) cur_episode.get(last_attribute);
                    String[] new_attr_array = append_attr_array(cur_attr_array, line);
                    cur_episode.put(last_attribute, new_attr_array);
                }

                last_content = line;
            }
            else if (starts_with_space && amount_leading_space == 9) {  // Specific
                line = line.replace('*', ' ').strip();
                JSONObject cur_season = info.getJSONObject(last_season);
                JSONObject cur_episode = cur_season.getJSONObject(last_episode);
                JSONObject cur_attribute = cur_episode.getJSONObject(last_attribute);
                JSONObject cur_content = cur_attribute.getJSONObject(last_content);

                assert last_content != null;

                if (last_content.equals("Scene Specific")) {
                    cur_content.put(line, new JSONObject());
                }
                else {
                    cur_attribute.put(last_content, line);
                }

                last_specific = line;
            }
            else if (starts_with_space && amount_leading_space == 12) {
                line = line.replace('*', ' ').strip();
                JSONObject cur_season = info.getJSONObject(last_season);
                JSONObject cur_episode = cur_season.getJSONObject(last_episode);
                JSONObject cur_attribute = cur_episode.getJSONObject(last_attribute);
                JSONObject cur_content = cur_attribute.getJSONObject(last_content);
                cur_content.put(last_specific, line);
            }
        }

        // Remove extra stuff
        info.remove("Chapter Template");
        info.remove("Extra Songs");

        return info;
    }

    public static String[] append_attr_array(String[] attr_array, String line) {
        String[] new_attr_array = new String[attr_array.length + 1];

        System.arraycopy(attr_array, 0, new_attr_array, 0, attr_array.length);
        new_attr_array[attr_array.length] = line;

        return new_attr_array;
    }

    public static String[] get_items(JSONObject info, int item) {
        // 1 for characters, 2 for locations, 3 for songs

        String[] item_list = new String[0];
        Iterator<String> season_list = info.keys();

        while (season_list.hasNext()) {
            String season_name = season_list.next();
            JSONObject season = info.getJSONObject(season_name);

            Iterator<String> episode_list = season.keys();
            while (episode_list.hasNext()) {
                String episode_name = episode_list.next();
                JSONObject episode = season.getJSONObject(episode_name);

                if (item == 1) {
                    String[] ep_items = (String[]) episode.get("Characters");

                    for (String ep_item: ep_items) {
                        item_list = append_ep_item(item_list, ep_item);
                    }
                }
                else if (item == 2) {
                    String[] ep_items = (String[]) episode.get("Locations");

                    for (String ep_item: ep_items) {
                        item_list = append_ep_item(item_list, ep_item);
                    }
                }
                else if (item == 3) {
                    item_list = append_ep_item(
                            item_list, (String) episode.getJSONObject("Songs").get("Intro Song")
                    );

                    item_list = append_ep_item(
                            item_list, (String) episode.getJSONObject("Songs").get("Outro Song")
                    );

                    JSONObject ep_scenes = episode.getJSONObject("Songs").getJSONObject("Scene Specific");
                    Iterator<String> ep_scenes_names = ep_scenes.keys();
                    while (ep_scenes_names.hasNext()) {
                        String cur_scene = ep_scenes_names.next();

                        item_list = append_ep_item(
                                item_list, (String) ep_scenes.get(cur_scene)
                        );
                    }
                }
            }
        }

        return item_list;
    }

    public static String[] append_ep_item(String[] item_list, String ep_item) {
        String[] new_item_list = new String[item_list.length + 1];

        // Check to make sure item isn't already in the list
        for (String item : item_list) {
            if (item.equals(ep_item)) {
                return item_list;
            }
        }

        System.arraycopy(item_list, 0, new_item_list, 0, item_list.length);
        new_item_list[item_list.length] = ep_item;

        return new_item_list;
    }
}
