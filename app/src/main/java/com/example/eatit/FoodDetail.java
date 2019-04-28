package com.example.eatit;

import android.support.annotation.NonNull;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.cepheuen.elegantnumberbutton.view.ElegantNumberButton;
import com.example.eatit.Common.Common;
import com.example.eatit.Database.Database;
import com.example.eatit.Model.Food;
import com.example.eatit.Model.Order;
import com.example.eatit.Model.Rating;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import com.stepstone.apprating.AppRatingDialog;
import com.stepstone.apprating.listener.RatingDialogListener;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class FoodDetail extends AppCompatActivity implements RatingDialogListener {

    TextView food_name,food_price,food_description;
    ImageView food_image;
    CollapsingToolbarLayout collapsingToolbarLayout;
    FloatingActionButton btnCart,btnRating;
    ElegantNumberButton numberButton;
    RatingBar ratingBar;

    String foodId="";

    FirebaseDatabase database;
    DatabaseReference foods;
    DatabaseReference ratingTbl;

    Food currentFood;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_detail);

        //Firebase
        database = FirebaseDatabase.getInstance();
        foods = database.getReference("Foods");
        ratingTbl = database.getReference("Rating");

        //Init view
        numberButton = findViewById(R.id.number_button);
        btnCart = findViewById(R.id.btnCart);
        btnRating = findViewById(R.id.btnRating);
        ratingBar = findViewById(R.id.ratingBar);

        btnRating.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRatingDialog();
            }
        });


        btnCart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Database(getBaseContext()).addToCart(new Order(
                        foodId,
                        currentFood.getName(),
                        numberButton.getNumber(),
                        currentFood.getPrice(),
                        currentFood.getDiscount()
                ));

                Toast.makeText(FoodDetail.this, "Added to Cart",Toast.LENGTH_SHORT).show();
            }
        });

        food_description = findViewById(R.id.food_description);
        food_name = findViewById(R.id.food_name);
        food_price = findViewById(R.id.food_price);
        food_image = findViewById(R.id.food_image);

        collapsingToolbarLayout = findViewById(R.id.collapsing);
        collapsingToolbarLayout.setCollapsedTitleTextAppearance(R.style.CollapsedAppbar);
        collapsingToolbarLayout.setExpandedTitleTextAppearance(R.style.ExpandedAppbar);

        //Get food id from intent
        if(getIntent() != null)
            foodId = getIntent().getStringExtra("FoodId");
        if(!foodId.isEmpty())
        {
            if(Common.isConnectedtoInternet(getBaseContext()))
            {
                getFoodDetails(foodId);
                getFoodRating(foodId);
            }

            else
            {
                Toast.makeText(FoodDetail.this,"Please check your connection",Toast.LENGTH_SHORT).show();
                return;
            }
        }

    }

    private void getFoodRating(String foodId) {
        Query foodRating = ratingTbl.orderByChild("foodId").equalTo(foodId);

        foodRating.addValueEventListener(new ValueEventListener() {
            int count=0,sum=0;
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot postSnapshot: dataSnapshot.getChildren())
                {
                    Rating item = postSnapshot.getValue(Rating.class);
                    sum+=Integer.parseInt(item.getRateValue());
                    count++;
                }
                if(count !=0)
                {
                    float average = sum/count;
                    ratingBar.setRating(average);
                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void showRatingDialog() {
        new AppRatingDialog.Builder()
                .setPositiveButtonText("Submit")
                .setNegativeButtonText("Cancel")
                .setNoteDescriptions(Arrays.asList("Very Bad","Not Good","Quite Ok","Very Good","Excellent"))
                .setDefaultRating(1)
                .setTitle("Rate This Food")
                .setDescription("Please select some stars and give us your feedback")
                .setTitleTextColor(R.color.colorPrimary)
                .setDescriptionTextColor(R.color.colorPrimary)
                .setHint("Please write your comment here")
                .setHintTextColor(R.color.colorAccent)
                .setCommentTextColor(android.R.color.white)
                .setCommentBackgroundColor(R.color.colorPrimaryDark)
                .setWindowAnimation(R.style.RatingDialogFadeAnim)
                .create(FoodDetail.this)
                .show();


    }

    private void getFoodDetails(String foodId) {
        foods.child(foodId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                currentFood = dataSnapshot.getValue(Food.class);

                //Set Image
                Picasso.with(getBaseContext()).load(currentFood.getImage())
                        .into(food_image);

                collapsingToolbarLayout.setTitle(currentFood.getName());

                food_price.setText(currentFood.getPrice());

                food_name.setText(currentFood.getName());

                food_description.setText(currentFood.getDescription());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    @Override
    public void onNegativeButtonClicked() {

    }

    @Override
    public void onPositiveButtonClicked(int i, @NotNull String s) {
        //Get rating and upload to firebase
        final Rating rating = new Rating(Common.currentUser.getPhone(),
                foodId,
                String.valueOf(i),
                s);
        ratingTbl.child(Common.currentUser.getPhone()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
             if(dataSnapshot.child(Common.currentUser.getPhone()).exists())
             {
                 //Remove old value
                 ratingTbl.child(Common.currentUser.getPhone()).removeValue();
                 //Update new value
                 ratingTbl.child(Common.currentUser.getPhone()).setValue(rating);
             }
             else
             {
                 //update new value
                 ratingTbl.child(Common.currentUser.getPhone()).setValue(rating);
             }
             Toast.makeText(FoodDetail.this, "Thank you for your feedback", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
