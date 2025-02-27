// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: snakes.proto

package ru.levachev.messages;

public interface GameConfigOrBuilder extends
    // @@protoc_insertion_point(interface_extends:snakes.GameConfig)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <pre>
   * Ширина поля в клетках (от 10 до 100)
   * </pre>
   *
   * <code>optional int32 width = 1 [default = 40];</code>
   * @return Whether the width field is set.
   */
  boolean hasWidth();
  /**
   * <pre>
   * Ширина поля в клетках (от 10 до 100)
   * </pre>
   *
   * <code>optional int32 width = 1 [default = 40];</code>
   * @return The width.
   */
  int getWidth();

  /**
   * <pre>
   * Высота поля в клетках (от 10 до 100)
   * </pre>
   *
   * <code>optional int32 height = 2 [default = 30];</code>
   * @return Whether the height field is set.
   */
  boolean hasHeight();
  /**
   * <pre>
   * Высота поля в клетках (от 10 до 100)
   * </pre>
   *
   * <code>optional int32 height = 2 [default = 30];</code>
   * @return The height.
   */
  int getHeight();

  /**
   * <pre>
   * Количество клеток с едой, независимо от числа игроков (от 0 до 100)
   * </pre>
   *
   * <code>optional int32 food_static = 3 [default = 1];</code>
   * @return Whether the foodStatic field is set.
   */
  boolean hasFoodStatic();
  /**
   * <pre>
   * Количество клеток с едой, независимо от числа игроков (от 0 до 100)
   * </pre>
   *
   * <code>optional int32 food_static = 3 [default = 1];</code>
   * @return The foodStatic.
   */
  int getFoodStatic();

  /**
   * <pre>
   * Задержка между ходами (сменой состояний) в игре, в миллисекундах (от 100 до 3000)
   * </pre>
   *
   * <code>optional int32 state_delay_ms = 5 [default = 1000];</code>
   * @return Whether the stateDelayMs field is set.
   */
  boolean hasStateDelayMs();
  /**
   * <pre>
   * Задержка между ходами (сменой состояний) в игре, в миллисекундах (от 100 до 3000)
   * </pre>
   *
   * <code>optional int32 state_delay_ms = 5 [default = 1000];</code>
   * @return The stateDelayMs.
   */
  int getStateDelayMs();
}
