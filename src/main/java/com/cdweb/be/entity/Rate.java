package com.cdweb.be.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// NOTE: Rate entity is deprecated. Use Review entity instead.
// The database has no 'rates' table; ratings are stored in the 'reviews' table.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Rate {

  private Integer id;
  private Integer star;
  private String comment;
  private Product product;
}
