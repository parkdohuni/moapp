package com.example.village

import android.app.Activity
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.village.model.Post
import com.example.village.model.UserModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.lang.Exception
import java.util.*

class WriteActivity : AppCompatActivity() {
    lateinit var btnWrite : Button
    lateinit var btnUploadPic : ImageButton
    lateinit var etTitle : EditText
    lateinit var btnCategory : Button
    lateinit var etPrice : EditText
    lateinit var etBody : EditText

    lateinit var test_name : EditText
    lateinit var ivGoods_w : ImageView
    var imgUri: Uri? = null

    /* 데이터베이스 */
    var database = FirebaseFirestore.getInstance()

    /* 스토리지 */
    var storage = Firebase.storage
    var storageRef = storage.reference

    /* 현재 로그인한 사용자 */
    val user = Firebase.auth.currentUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.writing)

        btnWrite = findViewById(R.id.btnWrite)
        btnUploadPic = findViewById(R.id.btnUploadPic)
        etTitle = findViewById(R.id.etTitle)
        btnCategory = findViewById(R.id.btnCategory)
        etPrice = findViewById(R.id.etPrice)
        etBody = findViewById(R.id.etBody)

        ivGoods_w = findViewById(R.id.ivGoods_w)

        // 갤러리 열고, 사진 고르면 표시해주는 버튼
        btnUploadPic.setOnClickListener {
            openGallery()
        }

        // 작성 완료 버튼 누를 시, DB에 포스트 업로드
        btnWrite.setOnClickListener {
            var title = etTitle.text.toString()
            var price = etPrice.text.toString().toInt()
            var body = etBody.text.toString()

            // 닉네임과 uid도 같이 저장
            var uid = user!!.uid
            var nickname: String = "실패"    // user.displayName

            // 같은 uid를 가진 사용자 닉네임 DB에서 가져오기
            database.collection("users").get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot != null) {
                        for (document in documentSnapshot) {
                            val getData = document.toObject<UserModel>()

                            println(" ")
                            println(" ")
                            println("if문 안에 진입 : getData? " + getData.uid)
                            println("if문 안에 진입 : uid? " + uid)
                            println(" ")
                            println(" ")

                            if (getData!!.uid.contentEquals(uid)) {
                                nickname = getData.userName.toString()

                                postUpload(nickname!!, uid, imgUri, title, price, body) // 리스너 안에다 넣어야 되네..
                                break
                            }
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.w("DocSnippets", "Error getting documents: ", exception)
                }

            // document(uid)

            finish()
        }

        // 뒤로 가기 버튼
        var btnReturn = findViewById<ImageButton>(R.id.btnReturn)
        btnReturn.setOnClickListener {
            finish()
        }
    }

    // 갤러리 여는 함수
    private fun openGallery() {
        val intent: Intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.setType("image/*")
        startActivityForResult(intent, 1)   // 1 = 갤러리 열기
    }

    // 갤러리에서 가져온 이미지 표시
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            if(requestCode == 1) {
                imgUri = data?.data

                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imgUri)
                    ivGoods_w.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        else {
            Log.d("ActivityResult", "Error")
        }
    }

    // DB에 포스트 올리는 함수
    private fun postUpload(nickname: String, uid: String, imageUri: Uri?, title: String, price: Int, body: String) {
        var timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        var imageFileName = "IMAGE_" + timestamp + ".png"

        var imageRef = storageRef.child("images/").child(imageFileName)

        imageRef?.putFile(imageUri!!).addOnSuccessListener {
            var newPost = Post()

            val long_now = System.currentTimeMillis()
            val t_date = Date(long_now)     // 현재 시간을 Date 타입으로 변환
            val t_dateFormat = SimpleDateFormat("yyyy-MM-dd kk:mm:ss", Locale("ko", "KR"))

            // 닉네임과 uid도 같이 저장
            newPost.nickname = nickname
            newPost.uid = uid

            newPost.imageUrl = "gs://village-e6e1a.appspot.com/images/" + imageFileName
            newPost.title = title
            newPost.price = price
            newPost.body = body
            newPost.timestamp = System.currentTimeMillis()
            newPost.time = t_dateFormat.format(t_date)

            database.collection("user-posts")
                .add(newPost)
                .addOnSuccessListener {
                    // Toast.makeText(this, "데이터가 추가되었습니다", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { exception ->
                    Log.w("MainActivity", "Error getting documents: $exception")
                }
        }
    }
}