/*
*** Author  :   Ricardo  
*** File    :   Main.java
*** Package :   PACKAGE_NAME 
*** Project :   YYets
*** Date    :   9/7/17
*** Email   :   wuhao528@gmail.com
*** Function:
*/

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sun.istack.internal.Nullable;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class YYets {
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java YYets fileid season episode");
            System.out.println("You can find fileid from http://m.zimuzu.tv/index.html");
            return;
        }
        String id = args[0];
        String season = args[1];
        String episode = args[2];
        File file = getFieldID(id, episode, season);
        if (file != null) {
            download(file);
        }
    }

    @Nullable
    private static File getFieldID(String id, String episode, String season) {
        String url = "https://api1.ousns.net/index.php?accesskey=519f9cab85c8059d17544947k361a827&client=2&id=" +
                id + "&g=api%2Fv2&episode=" + episode + "&m=index&season=" + season + "&a=resource_item";
        OkHttpClient client = new OkHttpClient();
        try {
            Response resp = client.newCall(new Request.Builder().url(url).build()).execute();
            JSONObject data = JSON.parseObject(resp.body().string());
            if (data.containsKey("data")) {
                JSONObject data2 = data.getJSONObject("data");
                if (data2.containsKey("item_app")) {
                    JSONObject app = data2.getJSONObject("item_app");
                    String[] array = app.getString("name").split("=", -1);
                    String name = array[1].split("\\.", -1)[0] + "S" + season + "E" + episode + ".mp4";
                    String link = "https://www.zmzfile.com:9043/rt/route?fileid=" +
                            array[3].replace("|", "");
                    return new File(name, link);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    private static void download(File f) {
        String url = f.link;
        String fieldID = url.replace("https://www.zmzfile.com:9043/rt/route?fileid=", "");
        long totalReadLength = 0;
        long totalLength = 0;
        int blockSize = 4096;
        int encryptSize = 16;
        int rawSize = blockSize - encryptSize;
        Response resp = null;
        InputStream is = null;
        OkHttpClient client = new OkHttpClient();

        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(2, new SecretKeySpec(getMD5Arr(fieldID + "zm" + fieldID), "AES"));
            RandomAccessFile file = new RandomAccessFile(f.name, "rw");
            file.getChannel().tryLock();
            totalReadLength = file.length();
            totalReadLength -= totalReadLength % blockSize;
            file.seek(totalReadLength);
            resp = client.newCall(new Request.Builder().url(url).
                    header("RANGE", "bytes=" + totalReadLength + "-").build()).execute();
            int code = resp.code();
            if (resp.isSuccessful() && (code == 200 || code == 206)) {
                if (totalReadLength == 0) {
                    totalLength = Long.parseLong(resp.header("Content-Length", "1"));
                } else {
                    String header = resp.header("Content-Range", null);
                    if (header.contains("/")) {
                        totalLength = Long.parseLong(header.substring(header.lastIndexOf("/") + 1));
                    }
                }
                if (totalLength != 0) {
                    System.out.println("totalLength: " + totalLength);
                    is = resp.body().byteStream();
                    ByteBuffer allocate = ByteBuffer.allocate(16384);
                    byte[] bArr;
                    byte[] bArr2 = new byte[8192];
                    byte[] bArr3 = new byte[8192];
                    int position;
                    long j = 0;
                    System.out.println("Start download " + f.name);
                    while (true) {
                        int read = is.read(bArr2);
                        if (read != -1) {
                            if (j == 0) {
                                j = System.currentTimeMillis();
                            }
                            bArr = null;
                            if (allocate.position() == 0 && read == 8192) {
                                bArr = bArr2;
                            } else {
                                allocate.put(bArr2, 0, read);
                                position = allocate.position();
                                if (position >= 8192) {
                                    allocate.position(0);
                                    bArr = bArr2;
                                    allocate.get(bArr);
                                    if (position > 8192) {
                                        int i2 = position - 8192;
                                        allocate.get(bArr3, 0, i2);
                                        allocate.clear();
                                        allocate.put(bArr3, 0, i2);
                                    } else {
                                        allocate.clear();
                                    }
                                }
                                if (bArr != null) {
                                    for (int i = 0; i < bArr.length; i += blockSize) {
                                        file.write(cipher.doFinal(bArr, i, encryptSize));
                                        file.write(bArr, encryptSize + i, rawSize);
                                    }
                                    totalReadLength += bArr.length;
                                    if (System.currentTimeMillis() - j > 10000) {
                                        j = 0;
                                        System.out.println("Downloading: " +
                                                (totalReadLength * 100 / totalLength) + "%");
                                    }
                                }
                            }
                        } else {
                            break;
                        }
                    }
                    position = allocate.position();
                    if (position > 0) {
                        allocate.position(0);
                        bArr = new byte[position];
                        allocate.get(bArr);
                        if (position >= encryptSize) {
                            int i3 = 0;
                            for (int i = 0; i <= bArr.length - encryptSize; i += blockSize) {
                                file.write(cipher.doFinal(bArr, i, encryptSize));
                                i3 += encryptSize;
                                if (blockSize + i <= bArr.length) {
                                    file.write(bArr, encryptSize + i, rawSize);
                                    i3 += rawSize;
                                }
                            }
                            if (i3 < bArr.length) {
                                file.write(bArr, i3, bArr.length - i3);
                            }
                        } else {
                            file.write(bArr);
                        }
                        totalReadLength += bArr.length;
                        System.out.println("Download finished");
                    }
                }
            } else {
                System.out.println("Download failed");
            }
            if (file != null) {
                file.close();
            }
            if (is != null) {
                is.close();
            }
            if (resp != null) {
                resp.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Nullable
    private static byte[] getMD5Arr(String string) {
        try {
            return MessageDigest.getInstance("MD5").digest(string.getBytes());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
}

class File {
    public String name;
    public String link;

    public File(String name, String link) {
        this.name = name;
        this.link = link;
    }
}
