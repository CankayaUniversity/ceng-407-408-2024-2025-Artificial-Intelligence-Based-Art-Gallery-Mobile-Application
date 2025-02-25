@file:Suppress("DEPRECATION")

package com.example.socialmediaapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socialmediaapp.adapters.UsersAdapter
import com.example.socialmediaapp.modal.Users
import com.example.socialmediaapp.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Locale


class SearchFragment:Fragment() {

    private var recyclerView:RecyclerView?=null
    private var userAdapter:UsersAdapter?=null
    private var mUser:MutableList<Users>?=null
    private var  searchItem:EditText?=null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View?
    {
        // Inflate the layout for this fragment

        val view = inflater.inflate(R.layout.fragment_search,container,false)
        recyclerView = view.findViewById(R.id.recyclerview_search)
        recyclerView?.setHasFixedSize(true)
        recyclerView?.layoutManager = LinearLayoutManager(context)

        mUser = ArrayList()
        //to show a user on search
        // FIXME: Find a better way to init UsersAdapter Better
        userAdapter = context?.let{ UsersAdapter() }
        userAdapter?.setUserLIST(mUser as ArrayList<Users>)
        recyclerView?.adapter = userAdapter

        searchItem = view.findViewById(R.id.searchitem)

        Log.d("Inside","SearchFragment")

        searchItem!!.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Perform search operation here
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Not needed in most cases, but must be overridden
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
               if(searchItem!!.text.toString() == "")
               {

               }else{
                   recyclerView?.visibility = View.VISIBLE
                   retrieveUser()
                   searchUser(s.toString().toLowerCase(Locale.ROOT))
               }
            }
        })

        return view
    }

    private fun searchUser(input:String)
    {
        val query = FirebaseDatabase.getInstance().reference
            .child("Users")
            .orderByChild("username")
            .startAt(input)
            .endAt(input + "\uf8ff")

        query.addValueEventListener(object:ValueEventListener
        {
            override fun onCancelled(error: DatabaseError) {

            }
            override fun onDataChange(datasnapshot: DataSnapshot) {
                mUser?.clear()

                for(snapshot in datasnapshot.children)
                {
                    //searching all users
                    val user=snapshot.getValue(Users::class.java)
                    if(user!=null)
                    {
                        mUser?.add(user)
                    }
                }
                userAdapter?.notifyDataSetChanged()
            }
        })

    }

    private fun retrieveUser()
    {

        val usersSearchRef=FirebaseDatabase.getInstance().reference.child("Users")//table name:Users
        usersSearchRef.addValueEventListener(object:ValueEventListener
        {
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context,"Could not read from Database",Toast.LENGTH_LONG).show()
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (searchItem!!.text.toString() == "") {
                    mUser?.clear()
                    for (snapShot in dataSnapshot.children) {
                        val user = snapShot.getValue(Users::class.java)
                        if (user != null) {
                            mUser?.add(user)
                        }
                        userAdapter?.notifyDataSetChanged()
                    }
                }
            }
        })

    }

}