package com.ule.demo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SerializeUtils {

    //序列化用户
    public static void serializeUser(String name, HFUser user) throws IOException {
        new ObjectOutputStream(Files.newOutputStream(Paths.get(".temp/" + name + ".hfuser"))).writeObject(user);
    }

    //尝试反序列化
    public static HFUser tryDeserialize(String name) throws Exception {
        return !Files.exists(Paths.get(".temp/" + name + ".hfuser")) ? null :
                (HFUser) new ObjectInputStream(Files.newInputStream(Paths.get(".temp/" + name + ".hfuser"))).readObject();
    }

}
