package com.example.socialmediaapp.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmediaapp.modal.Users
import de.hdodenhof.circleimageview.CircleImageView
import android.view.View as AndroidViewView
import com.example.socialmediaapp.R
import com.example.socialmediaapp.fragments.ProfileFragment
import com.google.firebase.auth.FirebaseUser

class SearchUsersAdapter(private var mContext:Context,
    private var mUser:List<Users>,
    private var isFragment:Boolean=false):RecyclerView.Adapter<SearchUsersAdapter.ViewHolder>()
{
    private val firebaseUser:FirebaseUser?= com.google.firebase.auth.FirebaseAuth.getInstance().currentUser

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchUsersAdapter.ViewHolder{

        //to make user item available in search item
        val view=LayoutInflater.from(mContext).inflate(R.layout.user_item_layout,parent,false)
        return SearchUsersAdapter.ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return mUser.size
    }

    override fun onBindViewHolder(holder: SearchUsersAdapter.ViewHolder, position: Int)
    {
        //to display the user data
        val user=mUser[position]
        holder.userNameTextView.text = user.username
        //holder.userFullnameTextView.text = user.username
        Glide.with(holder.itemView.context).load(user.image).into(holder.userProfileImage)

        //to go to searched user's profile
        holder.useritem.setOnClickListener(object :AndroidViewView.OnClickListener{

            override fun onClick(v: AndroidViewView?){
                val pref=mContext.getSharedPreferences("PREFS",Context.MODE_PRIVATE).edit()
                pref.putString("profileid",user.userid)
                pref.apply()
                // This part of the code could be incorrect!!!!
                (mContext as FragmentActivity).supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment,ProfileFragment()).commit()
            }
        })


    }


    // Encapsulating ViewHolder
    class ViewHolder(@NonNull itemView: AndroidViewView):RecyclerView.ViewHolder(itemView)
    {
        // user_item_follow exits
        var followButton:Button=itemView.findViewById(R.id.user_item_follow)
        // user_item_search_username exits
        var userNameTextView:TextView=itemView.findViewById(R.id.user_item_search_username)
        // user_item exits
        var useritem:LinearLayout=itemView.findViewById(R.id.user_item)
        // user_item_search_fullname exits
        var userFullnameTextView:TextView=itemView.findViewById(R.id.user_item_search_fullname)
        // user_item_image exits
        var userProfileImage:CircleImageView=itemView.findViewById(R.id.user_item_image)


    }
}

