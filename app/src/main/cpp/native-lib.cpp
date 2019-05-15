#include <jni.h>
#include <string>
#include "AES/aes.c"
#include<syslog.h>

//CRYPT CONFIG
#define MAX_LEN (2*1024*1024)
#define ENCRYPT 0
#define DECRYPT 1
#define AES_KEY_SIZE 128
#define READ_LEN 10


//AES_IV
static unsigned char AES_IV[16] = { 0x74 ,0x68 ,0x69 ,0x73 ,0x20 ,0x69 ,0x74 ,0x20 ,0x74 ,0x68 ,0x65 ,0x20 ,0x6b ,0x65 ,0x79 ,0x2e };
//AES_KEY
/*static unsigned char AES_KEY[32] = { 0x62 ,0x72 ,0x65 ,0x61 ,0x6b ,0x6d ,0x65 ,0x69 ,0x66 ,
                                     0x75 ,0x63 ,0x61 ,0x6e ,0x62 ,0x62 ,0x79 ,0x62 ,0x72 ,
                                     0x65 ,0x61 ,0x6b ,0x6d ,0x65 ,0x69 ,0x66 ,0x75 ,0x63 ,
                                     0x61 ,0x6e ,0x62 ,0x62 ,0x79 };*/
//Key = this it the key.
static unsigned char AES_KEY[16] = { 0x74 ,0x68 ,0x69 ,0x73 ,0x20 ,0x69 ,0x74 ,0x20 ,0x74 ,0x68 ,0x65 ,0x20 ,0x6b ,0x65 ,0x79 ,0x2e };


extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_example_aesndkexample_MainActivity_crypt(JNIEnv *env, jobject instance, jbyteArray jarray,
                                                  jbyteArray jkey, jlong time, jint jmode) {
    //check input data
    unsigned int len = (unsigned int) ((env)->GetArrayLength(jarray));
    if (len <= 0 || len >= MAX_LEN) {
        return NULL;
    }
    

    unsigned char *data = (unsigned char*) (env)->GetByteArrayElements(
            jarray, NULL);
    unsigned char *key = (unsigned char*) (env)->GetByteArrayElements(
            jkey, NULL);
    if (!data) {
        return NULL;
    }

    unsigned int mode = (unsigned int) jmode;
    unsigned int rest_len = len % AES_BLOCK_SIZE;
    unsigned int padding_len = (
            (ENCRYPT == mode) ? (AES_BLOCK_SIZE - rest_len) : 0);
    unsigned int src_len = len + padding_len;

    unsigned char *input = (unsigned char *) malloc(src_len);
    memset(input, 0, src_len);
    memcpy(input, data, len);
    if (padding_len > 0) {
        memset(input + len, (unsigned char) padding_len, padding_len);
    }

    (env)->ReleaseByteArrayElements( jarray, (jbyte*)data, 0);

    unsigned char * buff = (unsigned char*) malloc(src_len);
    if (!buff) {
        free(input);
        return NULL;
    }
    memset(buff, src_len, 0);

    //set key & iv
    unsigned int key_schedule[44] = { 0 };
    //unsigned int key_schedule[AES_BLOCK_SIZE * 4] = { 0 };
    //aes_key_setup(AES_KEY, key_schedule, AES_KEY_SIZE);
    aes_key_setup(key, key_schedule, AES_KEY_SIZE);

    if (mode == ENCRYPT) {
        aes_encrypt_cbc(input, src_len, buff, key_schedule, AES_KEY_SIZE,
                        AES_IV);
    } else {
        aes_decrypt_cbc(input, src_len, buff, key_schedule, AES_KEY_SIZE,
                        AES_IV);
    }

    if (ENCRYPT != mode) {
        unsigned char * ptr = buff;
        ptr += (src_len - 1);
        padding_len = (unsigned int) *ptr;
        if (padding_len > 0 && padding_len <= AES_BLOCK_SIZE) {
            src_len -= padding_len;
        }
        ptr = NULL;
    }

    jbyteArray bytes = (env)->NewByteArray( src_len);
    (env)->SetByteArrayRegion( bytes, 0, src_len, (jbyte*) buff);

    free(input);
    free(buff);

    return bytes;
}